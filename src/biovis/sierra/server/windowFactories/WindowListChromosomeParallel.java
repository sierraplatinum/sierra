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
import biovis.sierra.data.windows.ChromosomeWindowRange;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowListChromosomeParallel
        extends WindowList {
    // temporary list for approach B
    private Map<String, List<Window>> windowList;

    /**
     * Constructor for window list
     *
     * @param dm data mapper storing mapping of data sets to file names and
     * type, as well as mapping between experiments and controls
     */
    public WindowListChromosomeParallel(DataMapper dm) {
        super(dm);
        //init window lists
        windowList = new HashMap<>();
    }

    /**
     * functions for approach B:
     * Create window list and count tags at the same time,
     * i.e., counting is performed before adding the windows to this list.
     * Chromosomes are done in parallel. Better parallelization
     * but unequal distribution of task due to differences in
     * the sizes of the chromosomes.
     */

    /**
     * add window to a preliminary list for each chromosome
     *
     * @param chr chromosome on which the window is located
     * @param w window object to add
     */
    public synchronized void addWindow(String chr, Window w) {
        windowList.get(chr).add(w);
    }

    /**
     * estimates the number of windows for each chromosome and reserves space in
     * the window list
     *
     * @param genome hash map mapping the chromosome identifier onto their
     * length
     * @param windowSize window size
     * @param offset offset to move windows along the genome
     * reserved
     */
    public void estimateWindowNumber(
            Map<String, Integer> genome,
            int windowSize,
            int offset
    ) {
        for (String chr : genome.keySet()) {
            int num = 0;
            int length = genome.get(chr);
            length -= windowSize - 1;
            num += (int) length / offset + 1;
            setWindowsListSize(chr, num);
        }
    }

    /**
     * Reserves space for preliminary window lists for each chromosome. num is
     * expected to be the upper bound of windows on each chromosome. The real
     * number can be less since windows without any tag should be omitted
     *
     * @param chr chromosome for which space should be reserved
     * @param num upper bound for number of windows
     */
    private void setWindowsListSize(String chr, int num) {
        ArrayList<Window> win = new ArrayList<>();
        win.ensureCapacity(num);
        windowList.put(chr, win);
    }

    /**
     * Converts the chromosomes specific lists into the global lists.
     * Flatten is required for efficient computation of remaining data such as
     * p-values and peaks. It also reduces memory requirements.
     */
    public void flattenList() {
        // clear final window list
        windows.clear();

        int first = 0;
        int last = 0;

        chromosomeMap = new HashMap<>();

        // fill window list
        List<Window> chrWindowList;
        for (String chr : windowList.keySet()) {
            //ensure vectors capacity to copy data
            chrWindowList = windowList.get(chr);

            //copy
            windows.ensureCapacity(windows.size() + chrWindowList.size());
            windows.addAll(chrWindowList);

            // chromosome map entry
            first = last;
            last = windows.size();
            chromosomeMap.put(chr, new ChromosomeWindowRange(first, last));

            //clear old
            chrWindowList.clear();
        }

        windowList.clear();
        System.gc();
    }
}
