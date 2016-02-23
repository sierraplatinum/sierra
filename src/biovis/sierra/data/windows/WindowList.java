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
 *****************************************************************************
 */
package biovis.sierra.data.windows;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.server.correlation.CorrelationItem;
import biovis.sierra.server.correlation.CorrelationList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
public class WindowList
  implements CorrelationList {

    protected Map<String, ChromosomeWindowRange> chromosomeMap;

    // window list created in all approaches
    protected ArrayList<Window> windows;

    // data mapper with data information
    protected DataMapper mapper;

    /**
     * Constructor for window list
     *
     * @param dm data mapper storing mapping of data sets to file names and
     * type, as well as mapping between experiments and controls
     */
    public WindowList(DataMapper dm) {
        mapper = dm;

        //init window lists
        windows = new ArrayList<>();
    }

    /**
     * Init after loading status.
     */
    public void init() {
        int first = 0;
        int last = 0;

        // fill window list
        chromosomeMap = new HashMap<>();
        String firstChr = windows.get(0).getChr();
        String chr = firstChr;
        for (Window window : windows) {
            chr = window.getChr();

            // chromosome map entry
            if (!chr.equals(firstChr)) {
                chromosomeMap.put(chr, new ChromosomeWindowRange(first, last));

                first = last + 1;
                firstChr = chr;
            }
            ++last;
        }
        chromosomeMap.put(chr, new ChromosomeWindowRange(first, last - 1));
    }

    /**
     * Functions run after creation of window list and counting of tags. Thus,
     * it is expected that windows hold the whole list of windows.
     */
    /**
     * Scales all counts in the experiments (not in the controls) by their
     * corresponding scaling factors resulting in an equally library size for
     * control and experiment. This is required since Poisson distribution only
     * models experiments with same library size as control.
     */
    public void scaleAllExperiments() {
        final List<Replicate> replicates = mapper.getReplicates();
        Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
        new ParallelForInt2(p2, 0, windows.size()).loop(
                new IterationInt() {
            @Override
            public void iteration(int index) {
                windows.get(index).scale(replicates);
            }
        });
    }

    /**
     * get window iterator.
     *
     * @return iterator over all windows
     */
    public List<Window> getWindows() {
        return windows;
    }

    /**
     *
     * @return
     */
    @Override
    public List<? extends CorrelationItem> getCorrelationItems() {
        return getWindows();
    }

    /**
     * get window iterator.
     *
     * @param start first window to return
     * @param end last window to return + 1
     * @return iterator over all windows
     */
    public List<Window> getWindows(
            int start,
            int end
    ) {
        if (end > windows.size()) {
            return windows.subList(start, windows.size());
        } else {
            return windows.subList(start, end);  // data mapper with data information
        }
    }

    /**
     * get number of windows
     *
     * @return number of windows
     */
    public int getSize() {
        return windows.size();
    }

    /**
     * Get current data mapper
     *
     * @return dataMapper
     */
    public DataMapper getDataMapper() {
        return mapper;
    }

    /**
     * set new data mapper
     *
     * @param mapper data mapper
     */
    public void setDataMapper(DataMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return "WindowList [windows="
               + (windows != null ? toString(windows, maxLen) : null)
               + ", mapper=" + mapper + "]";
    }

    /**
     * to String
     *
     * @param collection collection of elements
     * @param maxLen maximum length
     * @return string
     */
    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator();
             iterator.hasNext() && i < maxLen;
             i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * add window to window list containing ALL windows
     *
     * @param w window object that will be added to list of windows
     */
    public void addWindow(Window w) {
        windows.add(w);
    }

    /**
     * Get window ranges for chromosomes.
     *
     * @return
     */
    public Collection<String> getChromosomes() {
        return chromosomeMap.keySet();
    }

    /**
     * Get window ranges for chromosomes.
     *
     * @return
     */
    public Collection<ChromosomeWindowRange> getChromosomeWindows() {
        return chromosomeMap.values();
    }
}
