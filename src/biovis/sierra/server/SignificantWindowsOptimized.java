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
package biovis.sierra.server;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.SignificantWindowHistogram;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class SignificantWindowsOptimized {

    /**
     * Estimate distance to median of significant windows.
     * @param mapper data mapper
     * @param wl window list
     */
    public static void estimateDistanceToMedianOfSignificantWindows(
            DataMapper mapper,
            WindowList wl
    ) {
        List<Replicate> replicates = mapper.getReplicates();
        final int numberOfReplicates = replicates.size();

        // create cromosome to number of significant windows per replicate map
        Map<String, List<Integer>> sigWindows = new HashMap<>();
        for (String chr : wl.getChromosomes()) {
            List<Integer> repSig = new ArrayList<>();
            for (Replicate r : replicates) {
                repSig.add(0);
            }
            sigWindows.put(chr, repSig);
        }

        // count significant windows per replicate
        {
            String chr;
            for (Window w : wl.getWindows()) {
                chr = w.getChr();
                for (int i = 0; i < numberOfReplicates; i++) {
                    if (w.getPValueList()[i] < mapper.getPvaluecutoff()) {
                        sigWindows.get(chr).set(i, sigWindows.get(chr).get(i) + 1);
                    }
                }
            }
        }

        // store significant window counts in replicates
        for (int i = 0; i < numberOfReplicates; i++) {
            Map<String, Integer> sigWindowRep = new HashMap<>();
            for (String chr : sigWindows.keySet()) {
                sigWindowRep.put(chr, sigWindows.get(chr).get(i));
            }
            replicates.get(i).setSignificantWindowsChrWise(sigWindowRep);
        }

        // store medians chromosomewise
        Map<String, Integer> medians = new HashMap<>();
        for (String chr : sigWindows.keySet()) {
            List<Integer> sorted = new ArrayList<>(sigWindows.get(chr));
            Collections.sort(sorted);
            int median = sorted.get(sorted.size() / 2);
            medians.put(chr, median);
        }
        mapper.setSignifcantWindowMedianChrWise(medians);
        //System.err.println("medians size = "+medians.size() +" " +medians.toString());

        int[] sigCount = new int[numberOfReplicates];

        for (int i = 0; i < numberOfReplicates; i++) {
            sigCount[i] = 0;
            for (Window w : wl.getWindows()) {
                if (w.getPValueList()[i] < mapper.getPvaluecutoff()) {
                    sigCount[i]++;
                }
            }
            replicates.get(i).setSignificantWindows(sigCount[i]);
        }

        // Compute median
        List<Integer> sorted = new ArrayList<>();
        for (int i : sigCount) {
            sorted.add(i);
        }
        Collections.sort(sorted);
        int median = sorted.get(sorted.size() / 2);

        // Compute histogram
        int counter = 0;
        List<Integer> histX = new ArrayList<>();
        List<Integer> histY = new ArrayList<>();
        histX.add(sorted.get(0));
        histY.add(1);
        for (int i = 1; i < sorted.size(); i++) {
            if (histX.get(counter) == sorted.get(i)) {
                histY.set(counter, histY.get(counter) + 1);
            } else {
                counter++;
                histX.add(sorted.get(i));
                histY.add(1);
            }
        }
        SignificantWindowHistogram swh
                = new SignificantWindowHistogram(
                        histX,
                        histY,
                        median);

        mapper.setSignificantWindowHistogram(swh);
    }
}
