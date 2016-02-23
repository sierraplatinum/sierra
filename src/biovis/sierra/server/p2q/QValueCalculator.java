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
package biovis.sierra.server.p2q;

import biovis.sierra.data.windows.MergeSortDouble;

/**
 *
 * @author Lydia Mueller
 */
public abstract class QValueCalculator {

    protected int numberOfCores;
    
    protected int[] sorted;
    protected double[] pValues;

    /**
     * Constructor.
     *
     * @param numberOfCores numberOfCores
     */
    protected QValueCalculator(
            int numberOfCores
    ) {
        this.numberOfCores = numberOfCores;
    }

    /**
     * create a sorted list of p-values
     *
     * @param values
    */
    public void createSortedIndex(double[] values) {
        this.pValues = values;
        // create index to sort and values for sorting
        int[] index = new int[values.length];
        for (int i = 0; i < index.length; i++) {
            index[i] = i;
        }

        // Sort index list
        MergeSortDouble sorter = new MergeSortDouble();
        try {
            sorted = sorter.startSort(index, values, numberOfCores);
        } catch (Exception e) {
            e.printStackTrace();
            sorted = index;
        }
    }

    /**
     * Transform all p values into q values for the replicate
     *
     */
    abstract public double[] p2q();
}
