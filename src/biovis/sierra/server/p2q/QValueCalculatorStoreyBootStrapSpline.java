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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class QValueCalculatorStoreyBootStrapSpline
        extends QValueCalculatorStorey {

    private final int bootStraps = 1000;
    private Random rand = new Random();

    /**
     * Constructor.
     *
     * @param numberOfCores numberOfCores
     */
    public QValueCalculatorStoreyBootStrapSpline(
            int numberOfCores
    ) {
        super(numberOfCores);
    }

    /**
     * Transform all p values into q values for the replicate
     *
     */
    public double[] p2q() {
        double piZero = calcPiZero();
        if (piZero < 0) {
            QValueCalculator qvc = new QValueCalculatorStoreySimple(numberOfCores, sorted, pValues);
            return qvc.p2q();
        } else {
            return p2q(piZero);
        }
    }

    /**
     * Transform all p values into q values for the replicate
     *
     * @return piZero
     */
    private double calcPiZero() {
        double piZero = 0.0;
        List<Double> vals = bootStrap();
        if (vals == null) {
            return -1;
        }
        piZero = vals.get(vals.size() / 2);
        return piZero;
    }

    /**
     * Transform all p values into q values for the replicate
     *
     * @return piZero
     */
    private List<Double> bootStrap() {
        List<Double> piZeros = new ArrayList<>();
        TreeSet<Integer> indexTS = new TreeSet<>();
        if (bootStraps > pValues.length) {
            for (int i = 0; i < pValues.length; i++) {
                indexTS.add(i);
            }
        } else {
            while (indexTS.size() < bootStraps) {
                indexTS.add(rand.nextInt(pValues.length));
            }
        }
        int[] index = new int[indexTS.size()];
        int i = 0;
        while (indexTS.size() > 0) {
            index[i] = indexTS.pollFirst();
        }
        double piZero = estimatePiZero(index);
        if (piZero < 0) {
            return null;
        }
        piZeros.add(piZero);
        Collections.sort(piZeros);
        return piZeros;
    }

    /**
     * Estimate piZero
     *
     * @param index
     * @return piZero
     */
    private double estimatePiZero(int[] index) {
        double piZero = 0.0;
        double[] pvals = new double[index.length];
        for (int i = 0; i < index.length; i++) {
            pvals[i] = pValues[index[i]];
        }
        //fit natural cubic spline
        PolynomialCurveFitter pcf = PolynomialCurveFitter.create(3);
        double[] parameter = pcf.fit(makeObservations(pvals));
        //evaluate at 1.0 -> sum of all four parameters
        double x = 1.0;
        piZero = approximatePiZero(parameter, x);
        while (piZero < 0 || piZero > 1.0) {
            x -= 0.01;
            if (x < 0) {
                return -1;
            }
            piZero = approximatePiZero(parameter, x);
        }
        return piZero;
    }

    /**
     * Approximate piZero from cubic polynomial
     *
     * @param parameter
     * @param x
     * @return piZero
     */
    private double approximatePiZero(double[] parameter, double x) {
        return parameter[0] + x * (parameter[1] + x * (parameter[2] + x * parameter[3]));
    }

    /**
     * Make observations.
     *
     * @param pvalues
     * @return list of weighted observed points
     */
    private List<WeightedObservedPoint> makeObservations(double[] pvalues) {
        List<Integer> counts = new ArrayList<>();
        for (double p = 0.0; p <= 0.8; p += 0.001) {
            counts.add(0);
        }

        for (double pVal : pvalues) {
            int index = (int) (pVal * 1000);
            index = Math.min(index, counts.size() - 1);
            for (int j = 0; j <= index; j++) {
                counts.set(j, counts.get(j) + 1);
            }
        }

        int m = pvalues.length;
        List<WeightedObservedPoint> obs = new ArrayList<>();
        for (double p = 0.0; p <= 0.8; p += 0.001) {

            int index = (int) (p * 1000);
            double piLambda = (double) counts.get(index) / (m * (1.0 - p));
//            System.err.println("lambda=" + p + " pi=" + piLambda + " count = " + counts.get(index) + " index=" + index + " m=" + m);
            obs.add(new WeightedObservedPoint(1.0, p, piLambda));
        }
        return obs;
    }
}
