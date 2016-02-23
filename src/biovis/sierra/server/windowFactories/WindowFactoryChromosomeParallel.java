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
import biovis.sierra.data.Replicate;
import biovis.sierra.data.ReplicateDataSet;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.server.Commander.PeakCommander;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import parallel4.IterationInt;
import parallel4.Parallel2;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowFactoryChromosomeParallel
        extends WindowFactory {

    /**
     * Approach B:
     * create windows and count in parallel but parallelize only chromosomes
     */
    /**
     * construct windows and counts tags for all data sets. Only windows with at
     * least one tag in at least one data set are stored in the window list.
     * While creating the window list also the neighbor relationship is
     * established.
     *
     * @param mapper data mapper with mapping of experiments to controls and data set to data file
     * @param windowSize length of windows
     * @param windowOffset offset by which window start is moved to the next window
     * @param pc peak commander
     * @return WindowList with list of windows ready for calculations
     * @throws IOException Throws IOException if SamReader cannot close the file
     */
    //create and count at the same time, only windows with any replicate counts > 0 are saved parallel for all chromosomes
    public static WindowList constructWindows(
            DataMapper mapper,
            int windowSize,
            int windowOffset,
            PeakCommander pc
    ) throws IOException {

        Logger log = Logger.getLogger("Constructing windows and state");
        log.info("Constructing windows and state B");

        // reset progress
        resetProgress();

        //extract chromosomes and length from sam file
        Map<String, Integer> genome = WindowFactory.generateGenome(mapper);

        // create window list
        WindowListChromosomeParallel list = new WindowListChromosomeParallel(mapper);
        //estimate number of windows and create temporary storage objects in window list
        list.estimateWindowNumber(genome, windowSize, windowOffset);

        //get vector of chromosomes for interation
        Vector<String> chroms = new Vector<>(genome.keySet());

        final int chromosomeSize = chroms.size();
        log.info("start parallel chromosomes");

        //Sam reader array for the all data sets
        final SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();

        //make windows in parallel
        Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
        new ParallelForInt2(p2, 0, chromosomeSize).loop(new IterationInt() {
            @Override
            public void iteration(int index) {
                String chr = chroms.get(index);
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

                int windowStart = 0;
                int windowEnd = windowStart + windowSize - 1;
                while (windowEnd < genome.get(chr)) {
                    Window w = new Window(chr, windowStart, windowEnd,
                                          mapper.getNumberOfDataSets(),
                                          mapper.getReplicates().size());

                    //count
                    for (int currentDataSet = 0; currentDataSet < readers.length; currentDataSet++) {
                        w.count(currentDataSet, readers[currentDataSet]);
                    }

                    if (w.getTagSum() > 0) {
                        list.addWindow(chr, w);
                    }
                    windowStart += windowOffset;
                    windowEnd += windowOffset;
                }

                // Add last, incomplete window
                if (windowStart < genome.get(chr)) {
                    Window w = new Window(chr, windowStart, genome.get(chr) - 1,
                                          mapper.getNumberOfDataSets(),
                                          mapper.getReplicates().size());

                    //count
                    for (int currentDataSet = 0; currentDataSet < readers.length; currentDataSet++) {
                        w.count(currentDataSet, readers[currentDataSet]);
                    }

                    if (w.getTagSum() > 0) {
                        list.addWindow(chr, w);
                    }
                }

                //One chromosome is done
                if (pc != null) {
                    addProgress(0.9 * (1) / (chromosomeSize));
                    Object[] command = new Object[2];
                    command[0] = "setProgress";
                    command[1] = getProgress();
                    pc.sendCommand(command);
                }
            }
        });

        log.info("windows constructed");

        log.info("flatten window list");
        list.flattenList();
        log.info("window list flattened");

        log.info("window list generated");

        return list;
    }
}
