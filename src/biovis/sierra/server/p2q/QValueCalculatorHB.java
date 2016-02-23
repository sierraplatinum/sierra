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

/**
 * Bonferroni-Holmes correction.
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class QValueCalculatorHB
        extends QValueCalculator {

    /**
     * Constructor.
     *
     * @param numberOfCores numberOfCores
     */
    public QValueCalculatorHB(
            int numberOfCores
    ) {
        super(numberOfCores);
    }

    /**
     * Transform all p values into q values for the replicate
     *
     */
    @Override
    public double[] p2q() {
        int m = sorted.length;
        double[] qValues = new double[m];

        /*
         * BH correction = (m-i+1)*p
         * m = number of tests
         * i -> ith smallest pvalue
         * i starts with 1
         *
         * for i start with zero = (m-(i+1)+1)*p=(m-i-1+1)*p=(m-i)*p
         */

        for (int l = 0; l < sorted.length; l++) {
            double qVal = pValues[sorted[l]] * (m - l);
            qVal = Math.min(qVal, 1);
            qValues[sorted[l]] = qVal;
        }
        
        return qValues;
    }
}
