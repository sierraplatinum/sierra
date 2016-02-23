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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowListReaderSerial1ChunkParallel
        extends WindowList {
    // temporary list for approach F
    private Map<String, Map<Integer, Window>> windowList;

    private final static Comparator<Window> compareWindowByStart = (w1, w2) -> Integer.compare(w1.getStart(), w2.getStart());

    /**
     * Constructor for window list
     *
     * @param dm data mapper storing mapping of data sets to file names and
     * type, as well as mapping between experiments and controls
     */
    public WindowListReaderSerial1ChunkParallel(DataMapper dm) {
        super(dm);
        windowList = new Hashtable<>();
    }

    /**
     * functions for approach F:
     * Functions to generate window list in parallel using chunks of windows.
     *
     * Temporally, windows are stored in a map of maps.
     * Each map entry corresponds to a chromosome holding a map of windows.
     * Afterwards the maps of windows are sorted and joined to a single list
     * of maps
     *
     * temporary data structure
     *
     * chr1 ---> {{w1,w2,w3,w4,...,wn},{wn+1,wn+2,...,w2n},...}
     * . . .
     * chrm ---> {{w1,w2,w3,w4,...,wn},{wn+1,wn+2,...,w2n},...}
     */
    /**
     * Fill chromosome hashmap
     *
     * @param genome hash map with chromosome identifiers and lengths
     */
    public void initChromosome(
        Map<String, Integer> genome
    ) {
        for (String chr : genome.keySet()) {
            windowList.put(chr, new Hashtable<>());
        }
    }

    /**
     * get window from temporary window list
     *
     * @param chr chromosome location of the window
     * @param start start position
     * @return window on chr at start
     */
    public synchronized Window getWindow(String chr, int start) {
        Map<Integer, Window> startMap = windowList.get(chr);
        if (startMap != null) {
            return startMap.get(start);
        }

        return null;
    }

    /**
     * add window to temporary window list
     *
     * @param chr chromosome location of the window
     * @param start start position
     * @param w window object to add
     */
    public synchronized void addWindow(String chr, int start, Window w) {
        Map<Integer, Window> startMap = windowList.get(chr);
        if (startMap == null) {
            startMap = new Hashtable<>();
            startMap.put(start, w);
            windowList.put(chr, startMap);
        } else {
            Window wExists = startMap.get(start);
            if (wExists == null) {
                startMap.put(start, w);
            }
        }
    }

    /**
     * It resolves the temporarily created data structure into a single window
     * list and also creates the neighbor relationship between the windows.
     * Using queues storing the already processed windows close enough to be
     * neighbors, the neighbor relationship can dynamically be created and
     * updated. For each neighborship relation (1k, 5k, and 10k environment) a
     * queue is used. Since environments are centered at the midpoint of the
     * window, windows farther away from the current windows mid point than
     * environment size/2 nucleotides are removed from the queue.
     *
     */
    public void flattenList() {
        Logger log = Logger.getLogger("Flatten window list");

        // clear final window list
        windows.clear();

        // fill window list
        for (Map<Integer, Window> windowListParallelChr : windowList.values()) {
            log.info("Copy chromosome started");
            List<Window> windowListSorted = new ArrayList<>(windowListParallelChr.values());
            log.info("Copy chromosome finished");
            log.info("Sorting window list");
            windowListSorted.sort(compareWindowByStart);
            log.info("Window list sorted");

            //copy
            windows.addAll(windowListSorted);

            windowListSorted.clear();
            log.info("Finished flattening window list for chromosome");
        }

        windowList.clear();
        System.gc();
    }
}
