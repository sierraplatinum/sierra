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
package biovis.sierra.data;


import org.apache.commons.math3.distribution.PoissonDistribution;

/**
 *
 * @author Lydia Mueller
 */
public class ReplicateDataSetPoissonDistribution {

    private transient PoissonDistribution poissonDistribution = null;

    /**
     * Constructor.
     *
     * @param lambda
     */
    public ReplicateDataSetPoissonDistribution(
        double lambda
    ) {
        poissonDistribution = new PoissonDistribution(lambda);
    }

    /**
     * Get lambda from PoissonDistribution.
     *
     * @return lambda from PoissonDistribution
     */
    public double getLambdaFromPoisson() {
        if (poissonDistribution != null) {
          return poissonDistribution.getMean();
        } else {
          return 0.0;
        }
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
        if (poissonDistribution != null &&
            Math.abs(poissonDistribution.getMean() - lambda) < 0.01) {

            Double cum = poissonDistribution.cumulativeProbability((int) Math.round(counts));
            if (cum.isNaN()) {
                pval = 0.0; //if cummulative probability would be almost 1.0, apache may produce a NaN
            } else {
                pval -= cum;
            }
        } else {
            PoissonDistribution local = new PoissonDistribution(lambda);
            Double cum = local.cumulativeProbability((int) Math.round(counts));
            if (cum.isNaN()) {
                pval = 0.0; //if cummulative probability would be almost 1.0, apache may produce a NaN
            } else {
                pval -= cum;
            }
        }
        return pval;
    }

    /**
     * Get probability from poisson distribution.
     *
     * @param i position in poisson distribution
     * @return probability at i
     */
    public double getProbability(int i) {
        return poissonDistribution.probability(i);
    }
}
