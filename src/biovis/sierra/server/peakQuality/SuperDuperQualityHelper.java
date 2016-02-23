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
package biovis.sierra.server.peakQuality;

import biovis.sierra.data.QualityCounter;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.SamRecordIntervalIteratorFactory;

import biovis.sierra.data.peakcaller.IntBoxplotData;
import java.util.List;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class SuperDuperQualityHelper {

    public final static SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault();
    public final static SamRecordIntervalIteratorFactory samRecordIntervalIteratorFactory = new SamRecordIntervalIteratorFactory();

    /**
     * Compute boxplot of qualities.
     *
     * @param qualities qualities
     * @return boxplot
     */
    public static IntBoxplotData makeBoxplotData(List<Integer> qualities) {
        /*
         printQualities(qualities);
         */
        IntBoxplotData bp = new IntBoxplotData();
        //minimum
        int i = 0;
        while (i < qualities.size() && qualities.get(i) == 0) {
            i++;
        }
        bp.setMin(i);

        //maximum
        i = QualityCounter.PHRED_MAX;
        while (i >= 0 && qualities.get(i) == 0) {
            i--;
        }
        bp.setMax(i);

        //quartiles
        int total = 0;
        for (int j = 0; j <= QualityCounter.PHRED_MAX; j++) {
            total += qualities.get(j);
        }

        int s_l = (int) (0.25 * total);
        int s_m = (int) (0.5 * total);
        int s_h = (int) (0.75 * total);

        int sum = 0;

        for (int j = 0; j <= QualityCounter.PHRED_MAX; j++) {
            if (sum < s_l && s_l <= (sum + qualities.get(j))) {
                bp.setLowerQuartile(j);
            }
            if (sum < s_m && s_m <= (sum + qualities.get(j))) {
                bp.setMedian(j);
            }
            if (sum < s_h && s_h <= (sum + qualities.get(j))) {
                bp.setUpperQuartile(j);
            }
            sum += qualities.get(j);
        }
        return bp;
    }

    /**
     * Print qualities
     *
     * @param qualities qualities
     */
    public static void printQualities(List<Integer> qualities) {
        for (int i = 0; i <= QualityCounter.PHRED_MAX; i++) {
            System.err.println("Qualities[" + i + "]: " + qualities.get(i));
        }
    }
}
