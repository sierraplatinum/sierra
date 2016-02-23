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


import java.util.ArrayList;
import java.util.List;

import parallel4.IterationInt;
import parallel4.IterationParameter;
import parallel4.Parallel2;
import parallel4.ParallelForParameter;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;
import parallel4.Tuple;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class PearsonCorrelationParallelFast {

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
    public PearsonCorrelationParallelFast(
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
        final double[][] corrMatrix = new double[samples][samples];
        final List<Tuple<Integer, Integer>> pairs = new ArrayList<>();
        for (int iLoop = 0; iLoop < samples; iLoop++) {
            final int i = iLoop;
            corrMatrix[i][i] = 1.0;
            for (int jLoop = i + 1; jLoop < samples; jLoop++) {
                pairs.add(new Tuple<>(iLoop, jLoop));
            }
        }
        Parallel2 p2 = ParallelizationFactory.getInstance(numberOfCores);
        new ParallelForParameter<>(p2, pairs).loop(
                new IterationParameter<Tuple<Integer, Integer>>() {
            @Override
            public void iteration(Tuple<Integer, Integer> parameter) {
                double corr = calculateCorrelation(correlationList, parameter.getFirst(), parameter.getSecond());
                corrMatrix[parameter.getFirst()][parameter.getSecond()] = corr;
                corrMatrix[parameter.getSecond()][parameter.getFirst()] = corr;
            }
        }
        );
//        printCorrelationMatrix(corrMatrix, "BLUB");
        return corrMatrix;
    }

    /**
     *
     * @param corrMatrix correlation matrix
     * @param label label
     */
    private void printCorrelationMatrix(double[][] corrMatrix, String label) {
        synchronized (corrMatrix) {
            System.err.println("PearsonCorrelationParallel.calculateCorrelations " + label);
            for (int iLoop = 0; iLoop < samples; iLoop++) {
                for (int jLoop = 0; jLoop < samples; jLoop++) {
                    System.err.print(corrMatrix[iLoop][jLoop] + " ");
                }
                System.err.println("");
            }
        }
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
        Parallel2 p2 = ParallelizationFactory.getInstance(numberOfCores);
        new ParallelForInt2(p2, 0, samples).loop(
                new IterationInt() {
                    @Override
                    public void iteration(int i) {
                        double sum = 0.0;
                        NormalDistribution normal = new NormalDistribution();
                        for (CorrelationItem correlationItem : correlationList.getCorrelationItems()) {
                            double pval = pValueCorrection(correlationItem.getCorrelationValues()[i]);
                            sum += normal.inverseCumulativeProbability(pval);
                        }
                        sampleMeans[i] = sum / count;
                    }
                }
        );
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
        Parallel2 p2 = ParallelizationFactory.getInstance(numberOfCores);
        new ParallelForInt2(p2, 0, samples).loop(
                new IterationInt() {
                    @Override
                    public void iteration(int i) {
                        double sum = 0.0;
                        NormalDistribution normal = new NormalDistribution();
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
        );
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

    public double[][] getCorrMatrix() {
        return corrMatrix;
    }
}
