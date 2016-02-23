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
package biovis.sierra.server.correlation;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lydia Mueller
 */
public class PearsonCorrelation {

    private double[] sampleMeans;
    private double[] sampleStdDevs;
    private int samples;
    
    private double[][] corrMatrix;

    private int numberOfCores;

    /**
     * Constructor.
     *
     * @param samples
     * @param numberOfCores
     */
    public PearsonCorrelation(
            int samples,
            int numberOfCores
    ) {
        this.samples = samples;
        sampleMeans = new double[samples];
        sampleStdDevs = new double[samples];

        this.numberOfCores = numberOfCores;
    }

    /**
     * Calculate correlations.
     *
     * @param correlationList correlation list
     */
    public double [][] calculateCorrelations(
            CorrelationList correlationList
    ) {
        initSampleMeans(correlationList);
        initSampleStdDevs(correlationList);
        double[][] corrMatrix = new double[samples][samples];
        Logger logger = Logger.getLogger("Pearson");
        for (int i = 0; i < samples; i++) {
            corrMatrix[i][i] = 1.0;
            logger.log(Level.INFO, "corr a {0} : {1}", new Object[]{i, corrMatrix[i][i]});
            for (int j = i + 1; j < samples; j++) {
                double corr = calculateCorrelation(correlationList, i, j);
                corrMatrix[i][j] = corr;
                corrMatrix[j][i] = corr;
            }
            logger.log(Level.INFO, "corr b {0} : {1}", new Object[]{i, corrMatrix[i][i]});
        }
        logger.log(Level.INFO, "corr c {0} : {1}", new Object[]{0, corrMatrix[0][0]});

        return corrMatrix;
    }

    /**
     * Calculate correlations between i and j.
     *
     * @param correlationList correlation list
     * @param i i
     * @param j j
     */
    private double calculateCorrelation(
            CorrelationList correlationList,
            int i,
            int j
    ) {
        int count = correlationList.getSize();
        double corr = 0.0;
        //double small = 3.2*10000000*6;
        NormalDistribution normal = new NormalDistribution();
        for (CorrelationItem correlationItem : correlationList.getCorrelationItems()) {
            double pvalI = pValueCorrection(correlationItem.getCorrelationValues()[i]);
            double pvalJ = pValueCorrection(correlationItem.getCorrelationValues()[j]);

            double xValue = normal.inverseCumulativeProbability(pvalI) - sampleMeans[i];
            double yValue = normal.inverseCumulativeProbability(pvalJ) - sampleMeans[j];
            double divisor = (count - 1) * sampleStdDevs[i] * sampleStdDevs[j];
            if (divisor < 1e-17) {
                divisor = 1e-17;
            }
            corr += xValue * yValue / divisor;
        }

        return corr;
    }

    /**
     * Initialize sample means.
     *
     * @param correlationList correlation list
     */
    private void initSampleMeans(
            CorrelationList correlationList
    ) {
        final int count = correlationList.getSize();
        double sum = 0.0;
        NormalDistribution normal = new NormalDistribution();
        for (int i = 0; i < samples; i++) {
            sum = 0.0;
            for (CorrelationItem correlationItem : correlationList.getCorrelationItems()) {
                double pval = pValueCorrection(correlationItem.getCorrelationValues()[i]);
                sum += normal.inverseCumulativeProbability(pval);
            }
            sampleMeans[i] = sum / count;
        }
    }

    /**
     * Initialize sample standard deviations.
     *
     * @param correlationList correlation list
     */
    private void initSampleStdDevs(
            CorrelationList correlationList
    ) {
        final int count = correlationList.getSize();
        double sum = 0.0;
        NormalDistribution normal = new NormalDistribution();
        for (int i = 0; i < samples; i++) {
            sum = 0.0;
            for (CorrelationItem correlationItem : correlationList.getCorrelationItems()) {
                double pval = pValueCorrection(correlationItem.getCorrelationValues()[i]);
                double probit = normal.inverseCumulativeProbability(pval);
                probit -= sampleMeans[i];
                sum += probit * probit;
            }
            sum /= (count - 1);
            if (sum < 1e-17) {
                sum = 1e-17;
            }
            sampleStdDevs[i] = Math.sqrt(sum);
        }
    }

    /**
     * Compute corrected p-value.
     *
     * @param pval p-value
     * @return corrected p-value
     */
    private double pValueCorrection(double pval) {
        /*
         * since theory and real data differ, p-values of exact 0 and 1 occur even though they should not
         * p-values exact 0 or 1 have to be corrected to avoid -Infinity and Infinity during transformation into probits
         * 1/10 of p-value cut-off is added or substracted from p-values of 0.0 and 1.0, respectively
         */
        double small = 5e-15;
        if (pval == 0.0) {
            pval += small / 10.0;//mapper.getPvaluecutoff() / 10.0;
        } else if (pval == 1.0) {
            pval -= small / 10.0;//mapper.getPvaluecutoff() / 10.0;
        }
        return pval;
    }
}
