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
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public abstract class QValueCalculatorStorey
        extends QValueCalculator {

    /**
     * Constructor.
     *
     * @param numberOfCores numberOfCores
     */
    public QValueCalculatorStorey(
            int numberOfCores
    ) {
        super(numberOfCores);
    }

    /**
     * Transform all p values into q values for the replicate
     *
     * @param piZero piZero
     */
    protected double[] p2q(
            double piZero
    ) {
        int m = sorted.length;
        double[] qValues = new double[m];

        double qVal = pValues[sorted[m-1]] * piZero;
        qValues[sorted[m - 1]] = qVal;

        double lastQVal = qVal;
        for (int l = sorted.length - 2; l >= 0; l--) {
            qVal = pValues[sorted[l]] * piZero * m / (l + 1.0);
            lastQVal = qVal = Math.min(qVal, lastQVal);
            qValues[sorted[l]] = qVal;
        }
        
        return qValues;
    }
}
