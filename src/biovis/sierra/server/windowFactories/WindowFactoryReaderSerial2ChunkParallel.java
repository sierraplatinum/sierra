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
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.server.Commander.PeakCommander;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import parallel4.IterationInt;
import parallel4.Parallel2;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowFactoryReaderSerial2ChunkParallel
        extends WindowFactory {

    /**
     * Approach F:
     * - for each data set:
     *   - create and count in chunks of DEFAULT_CHUNK_SIZE windows for a better parallelization
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
        log.info("Constructing windows and state F-2");

        // reset progress
        resetProgress();

        //extract chromosomes and length from sam file
        //chunk size for parallelization
        final int chunkSize = WindowFactoryChunkMap.DEFAULT_CHUNK_SIZE;
        log.log(Level.INFO, "Chunk size: {0}", chunkSize);

        // construct TreeMap to find corresponding chromosome
        Map<String, Integer> genome = WindowFactory.generateGenome(mapper);
        final WindowFactoryChunkMap chrtree = new WindowFactoryChunkMap(genome, windowSize, windowOffset);
        final int chunks = chrtree.getChunks();
        log.info("genome chunks prepared for calculation");

        //number of data sets
        final int numberOfDataSets = mapper.getNumberOfDataSets();
        // window lists for each dataset
        final WindowListReaderSerial2ChunkParallel[] list = new WindowListReaderSerial2ChunkParallel[numberOfDataSets];
        //Sam reader array for the all data sets
        final SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();

        for (int currentDataSetLoop = 0; currentDataSetLoop < numberOfDataSets; currentDataSetLoop++) {
            log.log(Level.INFO, "Processing data set {0}: {1} start", new Object[]{currentDataSetLoop, mapper.getDataSetByTag(currentDataSetLoop).getDescription()});

            final int currentDataSet = currentDataSetLoop;
            list[currentDataSet] = new WindowListReaderSerial2ChunkParallel(mapper);

            //estimate number of windows and create temporary storage objects in window list
            list[currentDataSet].estimateWindowNumber(genome, windowSize, windowOffset, chunkSize);

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
                        while (windowStart < startNextChunk) {
                            if (windowEnd >= genome.get(chr)) {
                                // trim windows at the end of the genome
                                windowEnd = genome.get(chr) - 1;
                            }

                            if (windowEnd - windowStart < windowSize - windowOffset) {
                                // do not consider too small windows at the end
                                break;
                            }

                            Window w = new Window(chr, windowStart, windowEnd,
                                                  mapper.getNumberOfDataSets(),
                                                  mapper.getReplicates().size());

                            //count
                            w.count(currentDataSet, samReader);

                            if (w.getTagCount(currentDataSet) > 0) {
                                list[currentDataSet].addWindow(chr, numChunk, w);
                            }
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
                        addProgress(0.9 * (1) / (quotient));
                        Object[] command = new Object[2];
                        command[0] = "setProgress";
                        command[1] = getProgress();
                        pc.sendCommand(command);
                    }
                }
            });

            // Merge lists
            if (currentDataSet > 0) {
                log.log(Level.INFO, "Merge current data set {0}: {1} to master start", new Object[]{currentDataSetLoop, mapper.getDataSetByTag(currentDataSetLoop).getDescription()});

                list[0].merge(list[currentDataSet], currentDataSet);
                list[currentDataSet] = null;

                log.log(Level.INFO, "Merge current data set {0}: {1} to master end", new Object[]{currentDataSetLoop, mapper.getDataSetByTag(currentDataSetLoop).getDescription()});
            }

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
