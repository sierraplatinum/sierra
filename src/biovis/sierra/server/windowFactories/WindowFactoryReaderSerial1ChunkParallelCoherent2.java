/**
 *****************************************************************************
 * Copyright (c) 2015 Daniel Gerighausen, Lydia Mueller, and Dirk Zeckzer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************
 */
package biovis.sierra.server.windowFactories;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.QualityCounter;
import biovis.sierra.data.Read;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.server.Commander.PeakCommander;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.SamRecordIntervalIteratorFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import parallel4.IterationInt;
import parallel4.Parallel2;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowFactoryReaderSerial1ChunkParallelCoherent2
        extends WindowFactory {

    private static int CHUNK_SIZE = 10000;
    private static int QUEUE_END = 10000;

    private static SamRecordIntervalIteratorFactory samRecordIntervalIteratorFactory = new SamRecordIntervalIteratorFactory();

    // Comparator for start and end queue
    private static Comparator<Read> compareReadByEnd = (s1, s2) -> Integer.compare(s1.getEnd(), s2.getEnd());

    /**
     * Approach E:
     * - for each data set:
     * - create and count in chunks
     * - get one chunk from bam File
     * - store all reads in queue sorted by start
     * - store reads of window in queue sorted by end
     * - if number reads for window a larger than zero -> create window
     */
    /**
     * construct list of windows, only windows with at least one tag in at least
     * one experiment or background are kept
     *
     * @param mapper data mapper with mapping of experiments to controls and data set to data file
     * @param windowSize length of windows
     * @param windowOffset offset by which window start is moved to the next window
     * @param pc peak commander
     * @return WindowList with list of windows ready for calculations
     * @throws IOException Throws IOException if SamReader cannot close the file
     */
    public static WindowList constructWindows(
            DataMapper mapper,
            int windowSize,
            int windowOffset,
            PeakCommander pc,
            int chunkSize
    ) throws IOException {

        Logger log = Logger.getLogger("Constructing windows and state");
        log.info("Constructing windows and state; E-3");

        // reset progress
        resetProgress();

        //extract chromosomes and length from sam file
        //chunk size for parallelization
        
//        final int chunkSize = CHUNK_SIZE;
        log.log(Level.INFO, "Chunk size: {0}", chunkSize);

        // construct TreeMap to find corresponding chromosome
        Map<String, Integer> genome = WindowFactory.generateGenome(mapper);
        final WindowFactoryChunkMap chrtree = new WindowFactoryChunkMap(genome, windowSize, windowOffset);
        final int chunks = chrtree.getChunks();
        log.info("genome chunks prepared for calculation");

        //number of data sets
        final int numberOfDataSets = mapper.getNumberOfDataSets();
        // window list
        final WindowListReaderSerial1ChunkParallelCoherent2 list = new WindowListReaderSerial1ChunkParallelCoherent2(mapper);
        //Sam reader array for the all data sets
        final SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();

        for (int currentDataSetLoop = 0; currentDataSetLoop < numberOfDataSets; currentDataSetLoop++) {
            log.log(Level.INFO, "Processing data set {0}: {1} start", new Object[]{currentDataSetLoop, mapper.getDataSetByTag(currentDataSetLoop).getDescription()});

            final QualityCounter qualityCounter = new QualityCounter();

            final int currentDataSet = currentDataSetLoop;

            final int[] max = {0, 0};

            //make windows in parallel
            Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCoresWindowFactory());
            new ParallelForInt2(p2, 0, chunks).loop(new IterationInt() {
                @Override
                public void iteration(int index) {

                    // open sam reader
                    try (final SamReader samReader = samReaderDefaultFactory.open(mapper.getDataSetByTag(currentDataSet).getFile());) {
                        //start chunk for corresponding index --> first chunk on chromosome
                        int chrStartChunk = chrtree.floorKey(index);

                        //chromosome for current chunk
                        String chr = chrtree.get(chrStartChunk);

                        //relativ (to chromosome based) chunk number for index
                        int numChunk = index - chrStartChunk;

                        int startChunk = numChunk * chunkSize * windowOffset;
                        int startNextChunk = startChunk + chunkSize * windowOffset;
                        if (startNextChunk > genome.get(chr)) {
                            startNextChunk = genome.get(chr);
                        }
                        int windowStart = startChunk;
                        int windowEnd = windowStart + windowSize - 1;

                        // retrieve chunk elements from sam reader
                        List<Interval> intervalList = new ArrayList<>();
                        intervalList.add(new Interval(chr, windowStart, startNextChunk - windowOffset + windowSize - 1));
                        //Queue<SAMRecord> recordsStart;
                        Queue<Read> recordsEnd;
                        // prepare priority queues for counting
                        try (CloseableIterator<SAMRecord> overlapping = samRecordIntervalIteratorFactory.makeSamRecordIntervalIterator(samReader, intervalList, true)) {
                            // prepare priority queues for counting
                            recordsEnd = new PriorityQueue<>(QUEUE_END, compareReadByEnd);

                            // last unused SAM record from iterator
                            Read lastUnused = null;
                            SAMRecord record;

                            while (windowStart < startNextChunk) {
                                if (windowEnd >= genome.get(chr)) {
                                    // trim windows at the end of the genome
                                    windowEnd = genome.get(chr) - 1;
                                }

                                if (windowEnd - windowStart < windowSize - windowOffset) {
                                    // do not consider too small windows at the end
                                    break;
                                }

                                // remove records ending before start of window
                                while (!recordsEnd.isEmpty() && recordsEnd.peek().getEnd() < windowStart) {
                                    recordsEnd.poll();
                                }

                                if (lastUnused != null
                                    && lastUnused.getStart() <= windowEnd) {
                                    // outside previous window
                                    recordsEnd.add(lastUnused);
                                    lastUnused = null;
                                }

                                while (lastUnused == null && overlapping.hasNext()) {
                                    record = overlapping.next();
                                    lastUnused = new Read(record);
                                    if (startChunk <= lastUnused.getStart()
                                        && lastUnused.getStart() < startNextChunk) {
                                        qualityCounter.count(lastUnused);
                                    }

                                    if (lastUnused.getStart() <= windowEnd) {
                                        recordsEnd.add(lastUnused);
                                        lastUnused = null;
                                    } else {
                                        break;
                                    }
                                }

                                synchronized (max) {
                                    if (max[1] < recordsEnd.size()) {
                                        max[1] = recordsEnd.size();
                                    }
                                }

                                // count elements
                                double tags = (double) recordsEnd.size();
                                if (tags > 0) {
                                    // if window countains reads -> create Window
                                    Window w = list.getWindow(chr, windowStart);
                                    if (w == null) {
                                        w = new Window(chr, windowStart, windowEnd,
                                                       mapper.getNumberOfDataSets(),
                                                       mapper.getReplicates().size());
                                        list.addWindow(chr, windowStart, w);
                                    }
                                    w.setTagCount(currentDataSet, tags);
                                }

                                // move window
                                windowStart += windowOffset;
                                windowEnd += windowOffset;
                            }
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    //One chunk is done
                    /**/
                    if (pc != null) {
                        double quotient = (double) chunks * (double) numberOfDataSets;
                        addProgress(0.25 * (1) / (quotient));
                        Object[] command = new Object[2];
                        command[0] = "setProgress";
                        command[1] = getProgress();
                        pc.sendCommand(command);
                    }
                    /**/
                }
            });

            qualityCounter.adjust();
            mapper.getDataSetByTag(currentDataSetLoop).computeQuality(
                    qualityCounter.getTotalcounts(),
                    qualityCounter.getQualitycounts(),
                    qualityCounter.getTagCount());
            log.log(Level.INFO, "max queues for {0}: {1} -- {2}",
                    new Object[]{mapper.getDataSetByTag(currentDataSetLoop).getDescription(), max[0], max[1]});

            /*
             if (pc != null) {
             double quotient = (double) numberOfDataSets;
             addProgress(0.25 * (1) / (quotient));
             Object[] command = new Object[2];
             command[0] = "setProgress";
             command[1] = getProgress();
             pc.sendCommand(command);
             }
             */
//            System.gc();
            log.log(Level.INFO, "Processing data set {0} end", mapper.getDataSetByTag(currentDataSetLoop).getDescription());
        }

        log.info("windows constructed");

        log.info("flatten window list");
        list.flattenList();
        log.info("window list flattened");

        log.info("window list generated");

        return list;
    }
}
