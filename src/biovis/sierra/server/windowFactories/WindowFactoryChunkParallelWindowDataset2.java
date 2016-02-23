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

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.ReplicateDataSet;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.server.Commander.PeakCommander;
import java.util.List;
import java.util.Map;

import java.io.IOException;
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
public class WindowFactoryChunkParallelWindowDataset2
        extends WindowFactory {

    /**
     * Approach C-2:
     * create and count in chunks of DEFAULT_CHUNK_SIZE windows for a better parallelization
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
        log.info("Constructing windows and state C-2");

        // reset progress
        resetProgress();

        //extract chromosomes and length from sam file
        //chunk size for parallelization
        final int chunkSize = WindowFactoryChunkMap.DEFAULT_CHUNK_SIZE;
        log.log(Level.INFO, "Chunk size: {0}", WindowFactoryChunkMap.DEFAULT_CHUNK_SIZE);

        // construct TreeMap to find corresponding chromosome
        Map<String, Integer> genome = WindowFactory.generateGenome(mapper);
        final WindowFactoryChunkMap chrtree = new WindowFactoryChunkMap(genome, windowSize, windowOffset);
        final int chunks = chrtree.getChunks();
        log.info("genome chunks prepared for calculation");

        // create window list
        final WindowListChunkParallelWindowDataset list = new WindowListChunkParallelWindowDataset(mapper);
        //estimate number of windows and create temporary storage objects in window list
        list.estimateWindowNumber(genome, windowSize, windowOffset, chunkSize);

        //Sam reader array for the all data sets
        final SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();

        //make windows in parallel
        Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
        new ParallelForInt2(p2, 0, chunks).loop(new IterationInt() {
            @Override
            public void iteration(int index) {
                //start chunk for corresponding index --> first chunk on chromosome
                int chrStartChunk = chrtree.floorKey(index);
                //chromosome for current chunk
                String chr = chrtree.get(chrStartChunk);
                //relativ (to chromosome based) chunk number for index
                int numChunk = index - chrStartChunk;

                //Sam reader array for the all data sets
                List<Replicate> replicates = mapper.getReplicates();
                int tags = replicates.size();
                Replicate replicate;
                ReplicateDataSet rpds;
                SamReader[] readers = new SamReader[2 * tags];
                for (int i = 0; i < tags; i++) {
                    replicate = replicates.get(i);
                    rpds = replicate.getBackground();
                    readers[rpds.getIndex()] = samReaderDefaultFactory.open(rpds.getFile());
                    rpds = replicate.getExperiment();
                    readers[rpds.getIndex()] = samReaderDefaultFactory.open(rpds.getFile());
                }

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
                    for (int currentDataSet = 0; currentDataSet < readers.length; currentDataSet++) {
                        w.count(currentDataSet, readers[currentDataSet]);
                    }

                    if (w.getTagSum() > 0) {
                        list.addWindow(chr, numChunk, w);
                    }
                    windowStart += windowOffset;
                    windowEnd += windowOffset;
                }

                // close readers
                for (SamReader reader : readers) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                //One chunk is done
                if (pc != null) {
                    addProgress(0.9 * (1) / (chunks));
                    Object[] command = new Object[2];
                    command[0] = "setProgress";
                    command[1] = getProgress();
                    pc.sendCommand(command);
                }
            }
        });

        log.info("windows constructed");

        log.info("joining chunks to list");
        list.flattenList();
        log.info("windows constructed and prepared for calculation");

        log.info("window list generated");

        return list;
    }
}
