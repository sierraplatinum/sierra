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
package biovis.sierra.data.windows;

/**
 *
 * @author Daniel Gerighausen, Lydia Mueller, Dirk Zeckzer
 */
public class MergeSortDouble {

    // index to sort / sorted index
    private int[] index;
    // temporary array of semi-sorted index
    private int[] helper;
    // values for sorting the index
    private double[] values;

    // number of available cores for parallelization
    private Integer numberOfCores;

    /**
     * MergeSort
     *
     * @param low low index
     * @param high high index
     * @throws Exception in case of exception
     */
    void mergeSort( int low, int high) throws Exception {
        int length = high - low + 1;
        if (length <= 1) {
            return; // a list with only 1 element is already sorted
        }
        int mid = length / 2;

        // Divide
        if (numberOfCores > 0) {
            // sort the lower half using a child thread
            synchronized(numberOfCores) {
                --numberOfCores;
            }
            SortingThread bThread = new SortingThread(this, low, low + mid - 1);
            bThread.start();

            // sort the upper half in the parent thread
            mergeSort(low + mid, high);

            // parent-child synchronization
            bThread.join();
            synchronized(numberOfCores) {
                ++numberOfCores;
            }
        } else {
            // sequential merge sort
            // System.out.println("iterativ: "+(depth+1)+" "+low+" "+high);
            // sort the lower half
            mergeSort(low, low + mid - 1);
            // sort the upper half
            mergeSort(low + mid, high);
        }

        // merge the sorted lists
        merge(low, high);
    }

    /**
     * MergeSort: merge.
     *
     * @param low low index
     * @param high high index
     */
    void merge(int low, int high) {
        int mid = (high - low + 1) / 2;
        int merged = low;
        int firstHalf = low;
        int secondHalf = low + mid;

        while (merged <= high) {
            if (firstHalf <= low + mid - 1 && secondHalf <= high) {
                // both lists contain elements
                helper[merged++] = (values[index[firstHalf]] < values[index[secondHalf]]) ? index[firstHalf++] : index[secondHalf++];
            } else if (firstHalf <= low + mid - 1) {
                // only first half contains elements
                while (merged <= high) {
                    helper[merged++] = index[firstHalf++];
                }
            } else {
                // only second half contains elements
                while (merged <= high) {
                    helper[merged++] = index[secondHalf++];
                }
            }
        }

        // copy merged list back to index
        for (int i = low; i <= high; i++) {
            index[i] = helper[i];
        }
    }

    public int[] startSort(
      int[] data,
      double[] values,
      int numberOfCores
    ) throws Exception {
        // Double[]
        // a={86.0,29.0,29.5,20.0,91.3,78.6,73.2,13.7,98.3,85.7,90.5,19.2,98.1,66.7,12.1,41.5,36.6,33.6,19.2,94.2,27.1,49.5,23.6,97.2,94.9};
        this.index = data;
        this.helper = new int[index.length];
        this.values = values;
        this.numberOfCores = numberOfCores;

        mergeSort(0, index.length - 1);
        // printArray(sorted);
        return index;
    }
}

/**
 * Class for spawning new thread for partial list
 */
class SortingThread extends Thread {

    static int threadCount;

    MergeSortDouble parent;
    int threadID;
    int low = 0;
    int high = 0;

    /**
     * Constructor
     *
     * @param parent parent
     * @param low low
     * @param high high
     */
    public SortingThread(
            MergeSortDouble parent,
            int low, int high
    ) {
        this.parent = parent;
        this.low = low;
        this.high = high;
        this.threadID = threadCount++;
    }

    // thread entry point
    @Override
    public void run() {
        try {
            // System.out.println(this.getId()+"Tiefe "+ (depth+1));
            // System.out.println();
            parent.mergeSort(low, high);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
