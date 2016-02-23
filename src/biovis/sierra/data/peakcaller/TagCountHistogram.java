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
package biovis.sierra.data.peakcaller;

import java.util.ArrayList;

import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class TagCountHistogram {

    private ArrayList<Double> counts;
    private ArrayList<Double> bins;
    private int binSize;
    private int maxValue;
    private ArrayList<Integer> binXValue;

    /**
     * Constructor.
     *
     * @param wl window list
     * @param index replicate index
     */
    public TagCountHistogram(WindowList wl, int index) {
        counts = new ArrayList<>();
        bins = new ArrayList<>();
        binSize = 1;
        maxValue = 0;
        count(wl, index);
        normalize(wl.getSize());
    }

    /**
     * Count tags.
     *
     * @param wl window list
     * @param index replicate index
     */
    private void count(WindowList wl, int index) {
        for (Window w : wl.getWindows()) {
            int count = (int) Math.round(w.getTagCount(index));
            while (counts.size() <= count) {
                counts.add(0.0);
            }
            counts.set(count, counts.get(count) + 1.0);
        }
        maxValue = counts.size();
    }

    /**
     * Normalize counts.
     *
     * @param total normalization value
     */
    private void normalize(int total) {
        for (int i = 0; i < counts.size(); i++) {
            counts.set(i, counts.get(i) / total);
        }
    }

    public int getMaxCount() {
        return counts.size() - 1;
    }

    public double getCount(int index) {
        return counts.get(index);
    }

    /**
     * Bin counts to create histogram.
     */
    public void toBins() {
        bins.clear();
        int maxCount = counts.size() - 1;
        int maxIndex = (int) Math.ceil(Math.log10(maxCount)) + 1;
        binXValue = new ArrayList<>();
        binXValue.add(0);
        bins.add(0.0);
        int exp = 1;
        while (exp <= maxCount) {
            binXValue.add(exp);
            bins.add(0.0);
            exp *= 10;
        }
        if (maxIndex == bins.size()) {
            binXValue.add(exp);
            bins.add(0.0);
        }
        bins.set(0, bins.get(0) + counts.get(0));
        for (int i = 1; i < counts.size(); i++) {
            int index = (int) Math.ceil(Math.log10(i)) + 1;
            //System.err.println(index+" "+i+" "+Math.ceil(Math.log10(maxCount))+" "+bins.size()+" "+maxIndex);
            bins.set(index, bins.get(index) + counts.get(i));
        }
        counts = null;
    }

    public int getBinSize() {
        return binSize;
    }

    public void setBinSize(int binSize) {
        this.binSize = binSize;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public ArrayList<Double> getBins() {
        return bins;
    }

    public ArrayList<Integer> getBinXValue() {
        return binXValue;
    }
}
