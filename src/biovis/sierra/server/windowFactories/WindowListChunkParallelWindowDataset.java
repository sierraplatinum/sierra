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
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowListChunkParallelWindowDataset
        extends WindowList {

    // temporary list for approach C
    private Map<String, Vector< ArrayList<Window>>> windowList;

    /**
     * Constructor for window list
     *
     * @param dm data mapper storing mapping of data sets to file names and
     * type, as well as mapping between experiments and controls
     */
    public WindowListChunkParallelWindowDataset(DataMapper dm) {
        super(dm);
    }

    /**
     * functions for approach C:
     * Functions to generate window list in parallel using chunks of windows.
     * Temporally, windows are stored in a hash map of vectors of ArrayList.
     * Each hash map entry corresponds to a chromosome holding a vector.
     * Each of those vectors stores an ArrayList for each chunk located
     * on the chromosomes. Each chunk-specific ArrayList contains the window
     * objects. Only windows with at least one tag in at least one data set
     * should be added. Afterwards this data structure has to be resolved
     * to a single list and the neighbor relationship has to be created.
     *
     * temporary data structure
     *
     * chr1 ---> {{w1,w2,w3,w4,...,wn},{wn+1,wn+2,...,w2n},...}
     * . . .
     * chrm ---> {{w1,w2,w3,w4,...,wn},{wn+1,wn+2,...,w2n},...}
     */
    /**
     * add window to temporary window list
     *
     * @param chr chromosome location of the window
     * @param numChunk chunk of the window
     * @param w window object to add
     */
    public void addWindow(String chr, int numChunk, Window w) {
        windowList.get(chr).get(numChunk).add(w);
    }

    /**
     * estimates maximal number of windows and chunks and calls function to
     * reserve space in window list
     *
     * @param genome hash map with chromosome identifiers and lengths
     * @param windowSize window size used
     * @param offset offset to move windows along the genome
     * @param chunkSize chunk size
     */
    public void estimateWindowNumber(
            Map<String, Integer> genome,
            int windowSize,
            int offset,
            int chunkSize
    ) {
        windowList = new HashMap<>(genome.size());
        for (String chr : genome.keySet()) {
            int num = 0;
            int length = genome.get(chr);
            length -= windowSize - 1;
            num += (int) length / offset + 1;
            int chunksNum = num / chunkSize;
            if ((chunksNum * chunkSize) > num) {
                chunksNum++;
            }
            setWindowsListSize(chr, chunksNum, chunkSize);
        }
    }

    /**
     * Reserve enough space for each chromosome and chunk. Initially, space for
     * only chunkSize/4 elements is reserved. In this way, maximal chunkSize is
     * reached by java internal resize strategy.
     *
     * @param chr chromosome name
     * @param chunksNum number of chunks on chromosome
     * @param chunkSize chunk size: maximal number of windows in chunk
     */
    private void setWindowsListSize(String chr, int chunksNum, int chunkSize) {
        Vector< ArrayList<Window>> chrom = new Vector<>();
        chrom.ensureCapacity(chunksNum);
        for (int i = 0; i <= chunksNum; i++) {
            chrom.add(new ArrayList<>());
            chrom.get(i).ensureCapacity(chunkSize / 4);
        }
        windowList.put(chr, chrom);
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
        // clear final window list
        windows.clear();

        int first = 0;
        int last = 0;
        String chr;
        Vector<ArrayList<Window>> windowListParallelChr;

        // fill window list
        chromosomeMap = new HashMap<>();
        for (Map.Entry<String, Vector<ArrayList<Window>>> windowListParallelChrEntry : windowList.entrySet()) {
            chr = windowListParallelChrEntry.getKey();
            windowListParallelChr = windowListParallelChrEntry.getValue();
            for (ArrayList<Window> windowListParallelChunk : windowListParallelChr) {
                int chunksize = windowListParallelChunk.size();
                windows.ensureCapacity(windows.size() + chunksize);
                for (Window currentWindow : windowListParallelChunk) {
                    //add window to list
                    windows.add(currentWindow);
                }
                windowListParallelChunk.clear();
            }

            windowListParallelChr.clear();

            // chromosome map entry
            first = last;
            last = windows.size();
            chromosomeMap.put(chr, new ChromosomeWindowRange(first, last));

            //log.info("Finished flattening window list for chromosome");
        }

        windowList.clear();
//        System.gc();
    }
}
