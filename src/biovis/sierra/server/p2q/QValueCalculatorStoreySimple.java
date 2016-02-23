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
import java.util.List;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class QValueCalculatorStoreySimple
        extends QValueCalculatorStorey {

    /**
     * Constructor.
     *
     * @param numberOfCores numberOfCores
     */
    public QValueCalculatorStoreySimple(
            int numberOfCores
    ) {
        super(numberOfCores);
    }

    /**
     * Constructor.
     *
     * @param numberOfCores numberOfCores
     * @param sorted
     * @param pValues
     */
    public QValueCalculatorStoreySimple(
            int numberOfCores,
            int[] sorted,
            double[] pValues
    ) {
        super(numberOfCores);
        this.sorted = sorted;
        this.pValues = pValues;
    }

    /**
     * Transform all p values into q values for the replicate
     *
     */
    public double[] p2q(
    ) {
        double piZero = simplePiZero();
        return p2q(piZero);
    }

    /**
     * Compute piZero using simple approach.
     *
     * @return piZero
     */
    private double simplePiZero() {
        double piZero = 0.0;
        List<Integer> counts = new ArrayList<>();
        for (double p = 0.0; p < 0.5001; p += 0.001) {
            //System.err.println("counts "+counts.size()+" p="+p);
            counts.add(0);
        }

        int m = 0;
        for (int i = 0; i < pValues.length; ++i) {
            m++;
            double pVal = pValues[i];
            int index = (int) (pVal * 1000);
            index = Math.min(index, counts.size() - 1);
            for (int j = 0; j <= index; j++) {
                counts.set(j, counts.get(j) + 1);
            }
        }
        //System.err.println("length of counts="+counts.size()+" first access will be " +(int)(0.5*1000));

        for (double p = 0.5; p >= 0.0; p -= 0.001) {
            int index = (int) (p * 1000);
            double piLambda = (double) counts.get(index) / (m * (1.0 - p));
            if (piLambda <= 1.0) {
                piZero = piLambda;
                break;
            }
        }
        return piZero;
    }
}
