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
public class WindowFactoryReaderSerial2ChunkParallelCoherent
        extends WindowFactory {

    private static int CHUNK_SIZE = 10000;
    private static int QUEUE_START = 300000;
    private static int QUEUE_END = 100000;

    private static SamRecordIntervalIteratorFactory samRecordIntervalIteratorFactory = new SamRecordIntervalIteratorFactory();

    // Comparator for start and end queue
    private static Comparator<SAMRecord> compareSAMRecordByStart = (s1, s2) -> Integer.compare(s1.getStart(), s2.getStart());
    private static Comparator<SAMRecord> compareSAMRecordByEnd = (s1, s2) -> Integer.compare(s1.getEnd(), s2.getEnd());

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
            PeakCommander pc
    ) throws IOException {

        Logger log = Logger.getLogger("Constructing windows and state");
        log.info("Constructing windows and state; E-1");

        // reset progress
        resetProgress();

        //extract chromosomes and length from sam file
        //chunk size for parallelization
        final int chunkSize = CHUNK_SIZE;
        log.log(Level.INFO, "Chunk size: {0}", chunkSize);

        // construct TreeMap to find corresponding chromosome
        Map<String, Integer> genome = WindowFactory.generateGenome(mapper);
        final WindowFactoryChunkMap chrtree = new WindowFactoryChunkMap(genome, windowSize, windowOffset);
        final int chunks = chrtree.getChunks();
        log.info("genome chunks prepared for calculation");

        //number of data sets
        final int numberOfDataSets = mapper.getNumberOfDataSets();
        // window lists for each dataset
        final WindowListReaderSerial2ChunkParallelCoherent[] list = new WindowListReaderSerial2ChunkParallelCoherent[numberOfDataSets];
        //Sam reader array for the all data sets
        final SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();

        for (int currentDataSetLoop = 0; currentDataSetLoop < numberOfDataSets; currentDataSetLoop++) {
            log.log(Level.INFO, "Processing data set {0}: {1} start", new Object[]{currentDataSetLoop, mapper.getDataSetByTag(currentDataSetLoop).getDescription()});

            final QualityCounter qualityCounter = new QualityCounter();

            final int currentDataSet = currentDataSetLoop;
            list[currentDataSet] = new WindowListReaderSerial2ChunkParallelCoherent(mapper);

            //estimate number of windows and create temporary storage objects in window list
            list[currentDataSet].estimateWindowNumber(genome, windowSize, windowOffset, chunkSize);

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
                        Queue<SAMRecord> recordsStart;
                        Queue<SAMRecord> recordsEnd;
                        // prepare priority queues for counting
                        try (CloseableIterator<SAMRecord> overlapping = samRecordIntervalIteratorFactory.makeSamRecordIntervalIterator(samReader, intervalList, true)) {
                            // prepare priority queues for counting
                            recordsStart = new PriorityQueue<>(QUEUE_START, compareSAMRecordByStart);
                            recordsEnd = new PriorityQueue<>(QUEUE_END, compareSAMRecordByEnd);
                            // insert records sorted by start value in start queue
                            while (overlapping.hasNext()) {
                                recordsStart.add(overlapping.next());
                            }
                            synchronized (max) {
                                if (max[0] < recordsStart.size()) {
                                    max[0] = recordsStart.size();
                                }
                            }
                        }

                        while (windowStart < startNextChunk) {
                            if (windowEnd >= genome.get(chr)) {
                                // trim windows at the end of the genome
                                windowEnd = genome.get(chr) - 1;
                            }

                            if (windowEnd - windowStart < windowSize - windowOffset) {
                                // do not consider too small windows at the end
                                break;
                            }

                            // remove SAMRecords ending before start of window
                            while (!recordsEnd.isEmpty() && recordsEnd.peek().getEnd() < windowStart) {
                                recordsEnd.poll();
                            }

                            // add SAMRecords starting before end of window
                            while (!recordsStart.isEmpty() && recordsStart.peek().getStart() <= windowEnd) {
                                recordsEnd.add(recordsStart.poll());
                            }

                            // count elements
                            double tags = (double) recordsEnd.size();
                            synchronized (max) {
                                if (max[1] < recordsEnd.size()) {
                                    max[1] = recordsEnd.size();
                                }
                            }

                            if (tags > 0) {
                                // if window countains reads -> create Window
                                Window w = new Window(chr, windowStart, windowEnd,
                                                      mapper.getNumberOfDataSets(),
                                                      mapper.getReplicates().size());
                                w.setTagCount(currentDataSet, tags);
                                list[currentDataSet].addWindow(chr, numChunk, w);
                            }

                            // move window
                            windowStart += windowOffset;
                            windowEnd += windowOffset;
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    //One chunk is done
                    if (pc != null) {
                        double quotient = (double) chunks * (double) numberOfDataSets;
                        addProgress(0.25 * (1) / (quotient));
                        Object[] command = new Object[2];
                        command[0] = "setProgress";
                        command[1] = getProgress();
                        pc.sendCommand(command);
                    }
                }
            });

            mapper.getDataSetByTag(currentDataSetLoop).computeQuality(
                    qualityCounter.getTotalcounts(),
                    qualityCounter.getQualitycounts(),
                    qualityCounter.getTagCount());
            log.log(Level.INFO, "max queues for {0}: {1} -- {2}",
                    new Object[]{mapper.getDataSetByTag(currentDataSetLoop).getDescription(), max[0], max[1]});

            // Merge lists
            if (currentDataSet > 0) {
                log.log(Level.INFO, "Merge current data set {0}: {1} to master start", new Object[]{currentDataSetLoop, mapper.getDataSetByTag(currentDataSetLoop).getDescription()});

                list[0].merge(list[currentDataSet], currentDataSet);
                list[currentDataSet] = null;

                log.log(Level.INFO, "Merge current data set {0}: {1} to master end", new Object[]{currentDataSetLoop, mapper.getDataSetByTag(currentDataSetLoop).getDescription()});
            }

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
            log.log(Level.INFO, "Processing data set {0} end", mapper.getDataSetByTag(currentDataSetLoop).getDescription());
        }

        log.info("windows constructed");

        log.info("flatten window list");
        list[0].flattenList();
        log.info("window list flattened");

        log.info("window list generated");

        return list[0];
    }
}
