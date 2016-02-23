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

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class InverseNormalMethod {

    // normal distribution with mean = 0 and sd  = 1
    private final static NormalDistribution normal = new NormalDistribution();

    private boolean correctCorrelation;
    private int numberOfActiveReplicates;
    private double[] weights;
    private boolean[] isActive;

    // significance level regulating factor as defined in "A Note on Combining Dependent Tests of Significance" by Joachim Hartung
    private final double kappa = 0.2;

    Logger logger = Logger.getLogger("InverseNormalMethod");

    /**
     * Constructor.
     *
     * @param correctCorrelation
     * @param isWeighted
     * @param replicates
     */
    public InverseNormalMethod(
            boolean correctCorrelation,
            boolean isWeighted,
            List<? extends INMReplicate> replicates
    ) {
        // correlation
        this.correctCorrelation = correctCorrelation; //dm.isCorrectCorrelation();
        numberOfActiveReplicates = 0;
        int nrReplicates = replicates.size();

        // set weights
        weights = new double[nrReplicates];
        isActive = new boolean[nrReplicates];
        INMReplicate replicate;
        for (int i = 0; i < weights.length; i++) {
            replicate = replicates.get(i);
            isActive[i] = replicate.isActive();
            if (isActive[i]) {
                ++numberOfActiveReplicates;
                if (isWeighted) {
                    weights[i] = replicate.getWeight();
                } else {
                    weights[i] = 1.0;
                }
            } else {
                weights[i] = 0.0;
            }
        }
    }

    /**
     * Compute p-value of normal distribution from p-values of Poisson-distributions.
     *
     * @param pVals p-values of the Poisson distributions
     * @return p-value of the normal distribution
     */
    public double getPValue(double[] pVals) {
        if (numberOfActiveReplicates < 1) {
            return 1;
        } else if (numberOfActiveReplicates == 1) {
            return pVals[0];
        } else {
            double finalPVal = 1.0;
            // calculate probits
            double[] probits = calculateProbits(pVals);
            // sum probits
            Double combined = sumProbits(probits);
            // calculate p-value based on normal distribution
            Double cum = normal.cumulativeProbability(combined);
            // Minimum possible for cum is 1e-16
            // assign 1e-17 if combined is not -inifinity
            // assign 1e-18 if combined is -infinity
            if (cum == 0.0) {
                if (combined < -20.0) {
                    cum = 1e-18;
                } else {
                    cum = 1e-17;
                }
            }
            /*
             Double cum = cum(combined);
             System.err.println(": Cum: " + cum + " " + combined + " " + (combined / (1 * Math.sqrt(2))));
             */
            if (cum.isNaN()) {
                if (combined > 0) {
                    finalPVal = 1.0;
                } else {
                    finalPVal = 0.0;
                }
            } else {
                finalPVal = cum;
            }
            return finalPVal;
        }
    }

    private double cum(double combined) {
        return 0.5 * (1 + erf(combined / (1 * Math.sqrt(2))));
    }

    private double erf(double x) {
        if (FastMath.abs(x) > 40) {
            return x > 0 ? 1 : -1;
        }
        final double ret = Gamma.regularizedGammaP(0.5, x * x, 1.0e-20, 100000);
        return x < 0 ? -ret : ret;
    }

    /**
     * Calculate probits from p-values of Poisson-distributions.
     * @param pVals p-values of Poisson-distributions.
     * @return probits
     */
    private double[] calculateProbits(double[] pVals) {
        // probits = Phi-1(p value) with Phi cumulative normal distribution
        double[] probits = new double[pVals.length];
        for (int i = 0; i < pVals.length; i++) {
            Double probit = 0.0;
            if (isActive[i]) {
                probit = normal.inverseCumulativeProbability(pVals[i]);
                if (probit.isInfinite()) {//unendlich ist bloed zum weiterrechnen
                    if (probit > 0) {
                        probit = Double.MAX_VALUE / numberOfActiveReplicates;
                    } else {
                        probit = -Double.MAX_VALUE / numberOfActiveReplicates;
                    }
                }
                if (probit.isInfinite()) {
                    logger.log(Level.SEVERE, "p-Value {0} results in  infinite value", pVals[i]);
                }
                if (probit.isNaN()) {
                    logger.log(Level.SEVERE, "p-Value {0} results in NaN", pVals[i]);
                }
            }

            probits[i] = probit;
        }

        return probits;
    }

    /**
     * Compute normalized sum of probits.
     *
     * @param probits probits
     * @return normalized sum of probits
     */
    private double sumProbits(double[] probits) {
        double rho = 0.0;
        if (correctCorrelation) {
            rho = calculateRho(probits);
        }
        //combined = sum of probits divided by square root of number of probits
        double sumWeightedProbits = 0.0;
        double sumWeights = 0.0;
        double sumWeightsSquared = 0.0;
        for (int i = 0; i < probits.length; i++) {
            if (isActive[i]) {
                sumWeightedProbits += probits[i] * weights[i];
                sumWeights += weights[i];
                sumWeightsSquared += weights[i] * weights[i];
            }
        }
        double rhofactor = rho + kappa * Math.sqrt(2.0 / (numberOfActiveReplicates + 1)) * (1.0 - rho);
        double denominator = Math.sqrt(sumWeightsSquared + (sumWeights * sumWeights - sumWeightsSquared) * rhofactor);

        Double combinedProbit = sumWeightedProbits / denominator;

        /*
         logger.log(Level.INFO,
         "Combined: {0} {1} {2} {3} {4} {5}",
         new Object[]{
         combinedProbit,
         sumWeightedProbits,
         denominator,
         sumWeights,
         sumWeightsSquared,
         rhofactor
         }
         );
         */
        if (combinedProbit.isNaN()) {
            /**/
            logger.log(Level.SEVERE,
                       "Combined: {0} {1} {2} {3} {4} {5}",
                       new Object[]{
                           combinedProbit,
                           sumWeightedProbits,
                           denominator,
                           sumWeights,
                           sumWeightsSquared,
                           rhofactor
                       }
            );
            /**/
        }
        return combinedProbit;
    }

    /**
     * Calculate rho.
     *
     * @param probits probits
     * @return rho
     */
    private double calculateRho(double[] probits) {
        if (numberOfActiveReplicates <= 1) {
            return 1.0;
        }

        // compute mean
        Double mean = 0.0;
        for (int i = 0; i < probits.length; ++i) {
            if (isActive[i]) {
                mean += probits[i];
            }
        }
        if (mean.isInfinite()) {
            //logger.log(Level.SEVERE, "Mean is infinite: {0} {1} {2}", new Object[]{probits[0], probits[1], mean});
            return 1.0;
        }

        mean /= numberOfActiveReplicates;

        // compute squared std deviation (named q in the paper)
        Double s = 0.0;
        for (int i = 0; i < probits.length; ++i) {
            if (isActive[i]) {
                s += (probits[i] - mean) * (probits[i] - mean);
            }
        }
        s /= (numberOfActiveReplicates - 1);
        if (s.isInfinite()) {
//            logger.log(Level.SEVERE, "s-dev is infinite: {0} {1} {2}", new Object[]{probits[0], probits[1], s});
            return 1.0;
        }

        // compute rho
        Double rho_est = 1 - s;
        Double rho = Math.max(-1.0 / (numberOfActiveReplicates - 1), rho_est);
        if (rho.isInfinite()) {
            logger.log(Level.SEVERE, "Rho is infinite: {0} {1} {2}", new Object[]{rho, rho_est, s});
            return 1.0;
        }
        /*
         logger.log(Level.INFO, "Rho: {0} {1} {2} {3}", new Object[]{rho, mean, s, rho_est});
         */

        return rho;
    }
}
