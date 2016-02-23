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

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.ReplicateDataSet;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;

import java.util.Map;
import java.util.HashMap;

import org.apache.commons.math3.distribution.PoissonDistribution;

/**
 *
 * @author Lydia Mueller
 */
public class PoissonDistributionCollection {

    private Map<Integer, PoissonDistribution> poissonsCollection;
    private DataMapper mapper;

    /**
     * Constructor.
     *
     * @param dm data mapper
     */
    public PoissonDistributionCollection(DataMapper dm) {
        poissonsCollection = new HashMap<>();
        mapper = dm;
    }

    /**
     * Estimate normalized lambdas.
     *
     * @param windows list of windows
     * @param raw true iff raw data should be used
     */
    public void estimateLambdas(WindowList windows, boolean raw) {
        for (Replicate replicate : mapper.getReplicates()) {
            estimateLambdas(windows, replicate.getBackground(), raw);
            estimateLambdas(windows, replicate.getExperiment(), raw);
        }
    }

    /**
     * Estimate normalized lambdas.
     *
     * @param windows list of windows
     * @param tags tags
     */
    private void estimateLambdas(
            WindowList windows,
            ReplicateDataSet rpds,
            boolean raw
    ) {
        int tag = rpds.getIndex();
        double lambda = 0.0;

        for (Window w : windows.getWindows()) {
            lambda += w.getTagCount(tag);
        }
        double windowCount = windows.getSize();
        if (raw) {
            poissonsCollection.put(tag, new PoissonDistribution(lambda / windowCount));
        }
        rpds.setLambda(lambda / windowCount, raw);
    }

    /**
     * Get lambda by tag-
     *
     * @param tag tag
     * @return lambda for tag
     */
    public double getLambdaByTag(int tag) {
        return poissonsCollection.get(tag).getMean();
    }

    /**
     * Compute and return p-value.
     *
     * @param tag tag
     * @param counts counts
     * @return p-value
     */
    public double getPValue(int tag, double counts) {
        double pval = 1.0;

        if (poissonsCollection.containsKey(tag)) {
            Double cum = poissonsCollection.get(tag).cumulativeProbability((int) Math.round(counts));
            if (cum.isNaN()) {
                pval = 0.0; //if cummulative probability would be almost 1.0, apache may produce a NaN
            } else {
                pval -= cum;
            }
        }
        return pval;
    }

    /**
     * Compute and return p-value.
     *
     * @param lambda lambda
     * @param counts counts
     * @return p-value
     */
    public double getPValue(double lambda, double counts) {
        double pval = 1.0;

        PoissonDistribution local = new PoissonDistribution(lambda);
        Double cum = local.cumulativeProbability((int) Math.round(counts));
        if (cum.isNaN()) {
            pval = 0.0; //if cummulative probability would be almost 1.0, apache may produce a NaN
        } else {
            pval -= cum;
        }
        return pval;
    }

    /**
     * Compute and return p-value.
     *
     * @param tag tag
     * @param lambda lambda
     * @param counts counts
     * @return p-value
     */
    public double getPValue(int tag, double lambda, double counts) {
        double pval = 1.0;
        if (poissonsCollection.containsKey(tag) && Math.abs(poissonsCollection.get(tag).getMean() - lambda) < 0.01) {
            pval = getPValue(tag, counts);
        } else {
            pval = getPValue(lambda, counts);
        }
        return pval;
    }

    public Map<Integer, PoissonDistribution> getPoissonsCollection() {
        return poissonsCollection;
    }
}
