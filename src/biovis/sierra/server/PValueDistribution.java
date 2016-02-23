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
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import parallel4.Tuple;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class PValueDistribution {

    /**
     * Estimate p-value distribution.
     * @param mapper data mapper
     * @param wl window list
     */
    public static void estimatePValueDistribution(
            DataMapper mapper,
            WindowList wl
    ) {
        final int steps = 19;

        //make mid point intervals and counts
        List<Replicate> replicates = mapper.getReplicates();
        List<Tuple<Integer, Integer>> exponents;
        List<Tuple<Integer, List<Integer>>> allExponents = new ArrayList<>();
        for (int exp = 0; exp < steps; ++exp) {
            allExponents.add(new Tuple(exp - steps + 1, new ArrayList<>()));
        }

        for (Replicate replicate : replicates) {
            exponents = new ArrayList<>();
            for (int exp = 0; exp < steps; ++exp) {
                exponents.add(new Tuple<>(allExponents.get(exp).getFirst(), 0));
            }

            //count frequency of pvalues for each bin
            for (Window w : wl.getWindows()) {
                double pval = w.getPValueList()[replicate.getIndex()];

                int index = steps - 1 + (int) Math.log10(pval);
                index = Math.min(steps - 1, index); //put p-value of 1.0 also in last bin
                index = Math.max(index, 0); //put all very small values into 0.0 bin
                Tuple<Integer, Integer> bin = exponents.get(index);
                bin.setSecond(bin.getSecond() + 1);
                replicate.setMaxPValueExp(bin.getSecond());
            }

            replicate.setFinalPValueExp(exponents);

            for (int exp = 0; exp < steps; ++exp) {
                allExponents.get(exp).getSecond().add(exponents.get(exp).getSecond());
            }
        }

        // compute median exponents over all replicates
        List<Tuple<Integer, Integer>> medianExponents = new ArrayList<>(allExponents.size());
        Integer median;
        for (int exp = 0; exp < steps; ++exp) {
            List<Integer> sortedList = allExponents.get(exp).getSecond();
            Collections.sort(sortedList);
            median = sortedList.get(sortedList.size() / 2);
            medianExponents.add(new Tuple(allExponents.get(exp).getFirst(), median));
        }
        mapper.setPValueMedianExponents(medianExponents);
    }

    /**
     * Estimate final p-value distribution.
     * @param mapper data mapper
     * @param wl window list
     */
    public static void estimateFinalPValueDistribution(
            DataMapper mapper,
            WindowList wl
    ) {
        final int steps = 19;

        //make mid point intervals and counts
        List<Tuple<Integer, Integer>> exponents = new ArrayList<>();
        for (int exp = 0; exp < steps; ++exp) {
            exponents.add(new Tuple<>(exp - steps + 1, 0));
        }

        //count frequency of pvalues for each bin
        for (Window w : wl.getWindows()) {
            double pval = w.getFinalPValue();

            int index = steps - 1 + (int) Math.log10(pval);
            index = Math.min(steps - 1, index); //put p-value of 1.0 also in last bin
            index = Math.max(index, 0); //put all very small values into 0.0 bin
            Tuple<Integer, Integer> bin = exponents.get(index);
            bin.setSecond(bin.getSecond() + 1);
            mapper.setMaxPValueExp(bin.getSecond());
        }

        mapper.setFinalPValueExp(exponents);
    }
}
