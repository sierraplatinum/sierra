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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowListReaderSerial2ChunkParallelCoherent
        extends WindowList {
    // temporary list for approach E
    private HashMap<String, Vector< ArrayList<Window>>> windowList;

    /**
     * Constructor for window list
     *
     * @param dm data mapper storing mapping of data sets to file names and
     * type, as well as mapping between experiments and controls
     */
    public WindowListReaderSerial2ChunkParallelCoherent(DataMapper dm) {
        super(dm);
    }

    /**
     * functions for approach E:
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
    public void setWindowsListSize(String chr, int chunksNum, int chunkSize) {
        Vector< ArrayList<Window>> chrom = new Vector<>();
        chrom.ensureCapacity(chunksNum);
        for (int i = 0; i <= chunksNum; i++) {
            chrom.add(new ArrayList<>());
            chrom.get(i).ensureCapacity(chunkSize / 4);
        }
        windowList.put(chr, chrom);
    }

    /**
     * Merge two window lists
     *
     * @param windowList2 window list to merge
     * @param dataset dataset number of windowList2
     */
    public void merge(
        WindowListReaderSerial2ChunkParallelCoherent windowList2,
        int dataset
    ) {
        ArrayList<Window> windowListChr1;
        ArrayList<Window> windowListChr2;
        ArrayList<Window> windowListMerged;

        Vector<ArrayList<Window>> windowListVector1;
        Vector<ArrayList<Window>> windowListVector2;

        Window window1;
        Window window2;

        String chr;

        for (Map.Entry<String, Vector<ArrayList<Window>>> windowListEntries : windowList.entrySet()) {
            chr = windowListEntries.getKey();
            windowListVector1 = windowListEntries.getValue();
            windowListVector2 = windowList2.windowList.get(chr);
            int max1 = windowListVector1.size();
            int max2 = windowListVector2.size();
            int maxEntries = Math.max(max1, max2);
            for (int chunk = 0; chunk < maxEntries; ++chunk) {
                if (chunk < max1 && chunk < max2) {
                    windowListChr1 = windowListVector1.elementAt(chunk);
                    windowListChr2 = windowListVector2.elementAt(chunk);
                    windowListMerged = new ArrayList<>();

                    int sizeWindowList1 = windowListChr1.size();
                    int sizeWindowList2 = windowListChr2.size();
                    if (sizeWindowList1 > 0 && sizeWindowList2 > 0) {
                        windowListMerged.ensureCapacity((int) (Math.max(sizeWindowList1, sizeWindowList2) * 1.25));
                        int indexWindowList1 = 0;
                        int indexWindowList2 = 0;

                        while (indexWindowList1 < sizeWindowList1
                               && indexWindowList2 < sizeWindowList2) {
                            window1 = windowListChr1.get(indexWindowList1);
                            window2 = windowListChr2.get(indexWindowList2);
                            if (window1.getStart() == window2.getStart()) {
                                window1.setTagCount(dataset, window2.getTagCount(dataset));
                                windowListMerged.add(window1);
                                ++indexWindowList1;
                                ++indexWindowList2;
                            } else if (window1.getStart() < window2.getStart()) {
                                windowListMerged.add(window1);
                                ++indexWindowList1;
                            } else {
                                windowListMerged.add(window2);
                                ++indexWindowList2;
                            }
                        }

                        if (indexWindowList1 < sizeWindowList1) {
                            while (indexWindowList1 < sizeWindowList1) {
                                window1 = windowListChr1.get(indexWindowList1);
                                windowListMerged.add(window1);
                                ++indexWindowList1;
                            }
                        } else {
                            while (indexWindowList2 < sizeWindowList2) {
                                window2 = windowListChr2.get(indexWindowList2);
                                windowListMerged.add(window2);
                                ++indexWindowList2;
                            }
                        }

                        windowListChr1.clear();
                        windowListChr2.clear();
                        windowListVector1.set(chunk, windowListMerged);
                    } else if (sizeWindowList2 > 0) {
                        windowListVector1.set(chunk, windowListChr2);
                    }
                } else if (chunk < max2) {
                    windowListVector1.set(chunk, windowListVector2.elementAt(chunk));
                }
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
        // clear final window list
        windows.clear();

        // fill window list
        for (Vector<ArrayList<Window>> windowListParallelChr : windowList.values()) {
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
        }

        windowList.clear();
        System.gc();
    }
}
