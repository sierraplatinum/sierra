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
import biovis.sierra.data.windows.ChromosomeWindowRange;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.server.Commander.PeakCommander;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parallel4.IterationInt;
import parallel4.Parallel2;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowListCountTagParallel
        extends WindowList {

    /**
     * Constructor for window list
     *
     * @param dm data mapper storing mapping of data sets to file names and
     * type, as well as mapping between experiments and controls
     */
    public WindowListCountTagParallel(DataMapper dm) {
        super(dm);
    }

    /**
     * functions for approach A:
     * - construct windows first and count in parallel afterwards
     * - many windows with total tag count of 0
     * - construction not parallelized
     */
    /**
     * add window to window list containing ALL windows (not separated by
     * chromosome used in native approach but not efficient)
     *
     * @param w window object that will be added to list of windows
     */
    @Override
    public void addWindow(Window w) {
        windows.add(w);
    }

    /**
     * Estimates the number of windows
     *
     * @param genome hashmap mapping chromosomes on their length
     * @param windowSize window size
     * @param offset offset by which windows are moved along the genome
     */
    public void estimateWindowNumber(Map<String, Integer> genome, int windowSize, int offset) {
        Integer num = 0;
        for (String chr : genome.keySet()) {
            int length = genome.get(chr);
            length -= windowSize - 1;
            num += (int) length / offset + 1;
        }
        setWindowsListSize(num);
    }

    /**
     * - reserve window list with estimated amount of windows - num is accurate
     * but many windows saved in list are not required due to total count of 0
     *
     * @param num overall windows count
     */
    public void setWindowsListSize(int num) {
        windows.ensureCapacity(num);
    }

    /**
     * count all tags for all windows in parallel counting is carried out in
     * chunks of 10000 windows for a better distribution of the task. It
     * requires that the window list is already created
     *
     * @param pc peak commander
     * @throws IOException IOException is thrown when SAM/BAM file cannot be
     * closed
     */
    public void countAllTags(
            PeakCommander pc
    ) throws IOException {
        final SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();
        int stepsize = 10000;
        for (final Replicate replicate : mapper.getReplicates()) {
            final File backgroundFile = replicate.getBackground().getFile();
            final int backgroundIndex = replicate.getBackground().getIndex();

            final File experimentFile = replicate.getExperiment().getFile();
            final int experimentIndex = replicate.getExperiment().getIndex();

            Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
            new ParallelForInt2(p2, 0, windows.size(), stepsize).loop(new IterationInt() {
                @Override
                public void iteration(int index) {
                    try (SamReader samBackground = samReaderDefaultFactory.open(backgroundFile);
                         SamReader samExperiment = samReaderDefaultFactory.open(experimentFile);) {
                        int end = Math.min(index + stepsize, windows.size());
                        for (Window window : getWindows(index, end)) {
                            window.count(backgroundIndex, samBackground);
                            window.count(experimentIndex, samExperiment);
                        }

                        //One chunk is done
                        if (pc != null) {
                            WindowFactory.addProgress(0.9 * (1) / (windows.size() + 1) * stepsize / mapper.getReplicates().size());
                            Object[] command = new Object[2];
                            command[0] = "setProgress";
                            command[1] = WindowFactory.getProgress();
                            pc.sendCommand(command);
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Remove unused windows to reduce memory footprint.
     */
    public void flatten() {
        // remove unused windows and fill chromosome map
        List<Window> allWindows = windows;
        windows = new ArrayList<>();

        int first = 0;
        int last = 0;
        String chr = null;

        chromosomeMap = new HashMap<>();

        chr = allWindows.get(0).getChr();
        for (Window window : allWindows) {
            if (window.getTagSum() > 0) {
                windows.add(window);

                if (!chr.equals(window.getChr())) {
                    // chromosome map entry
                    first = last;
                    last = windows.size();
                    chromosomeMap.put(chr, new ChromosomeWindowRange(first, last));
                    chr = window.getChr();
                }
            }
        }
        allWindows.clear();
    }
}
