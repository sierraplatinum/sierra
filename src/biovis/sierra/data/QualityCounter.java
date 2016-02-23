/**
 *****************************************************************************
 * Copyright (c) 2015 Daniel Gerighausen, Lydia Mueller, and Dirk Zeckzer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************
 */
package biovis.sierra.data;

import htsjdk.samtools.SAMRecord;

/**
 *
 * @author Daniel Gerighausen, Lydia Mueller, Dirk Zeckzer
 */
public class QualityCounter {

    public static final int PHRED_MAX = 75;
    private int[] qualitycounts = new int[PHRED_MAX + 1];
    private long totalcounts = 0;
    private int tagCount = 0;
    private int min = PHRED_MAX;
    private int max = 0;

    /**
     * Constructor.
     */
    public QualityCounter() {
        for (int i = 0; i < qualitycounts.length; i++) {
            qualitycounts[i] = 0;
        }
    }

    /**
     * Compute quality counts for a list of Read.
     *
     * @param reads to add
     */
    public void count(
            Iterable<Read> reads
    ) {
        for (Read record : reads) {
            count(record);
        }
    }

    /**
     * Compute quality counts for Read.
     *
     * @param record to add
     */
    public void count(
            Read record
    ) {
        count(record.getReadUnmappedFlag(),
              record.getBaseQualities());
    }

    /**
     * Compute quality counts for a list of SAMRecord.
     *
     * @param reads to add
     */
    public void countSAM(
            Iterable<SAMRecord> reads
    ) {
        for (SAMRecord record : reads) {
            count(record);
        }
    }

    /**
     * Compute quality counts for SAMRecord.
     *
     * @param record to add
     */
    public void count(
            SAMRecord record
    ) {
        count(record.getReadUnmappedFlag(),
              record.getBaseQualities());
    }

    /**
     * Compute quality counts.
     *
     * @param readUnmappedFlag unmapped flag
     * @param qualities base qualities
     */
    synchronized private void count(
            boolean readUnmappedFlag,
            byte[] qualities
    ) {
        if (readUnmappedFlag == false) {
            tagCount++;
            for (byte b : qualities) {
                int qIndex = (int) b;
                if (qIndex > PHRED_MAX) {
                    System.err.println("PhredScore to high: " + qIndex);
                    qIndex = PHRED_MAX;
                }
                qualitycounts[qIndex]++;
                totalcounts++;

                if (min > qIndex) {
                    min = qIndex;
                }
                if (max < qIndex) {
                    max = qIndex;
                }
            }
        }
    }

    public void adjust() {
        if (max > 41) {
            // Illumina 1.3 or 1.5
            int current = 0;
            for (; current <= 41; ++current) {
                qualitycounts[current] = qualitycounts[current+31];
            }
            for (; current <= PHRED_MAX; ++current) {
                qualitycounts[current] = 0;
            }
        }
    }

    public int[] getQualitycounts() {
        return qualitycounts;
    }

    public long getTotalcounts() {
        return totalcounts;
    }

    public int getTagCount() {
        return tagCount;
    }

    public int getMedian() {
        //calculate median
        int medianSum = (int) (0.5 * totalcounts);
        int sum = 0;
        int medianQuality = 0;
        while (medianSum > (sum + qualitycounts[medianQuality])) {
            sum += qualitycounts[medianQuality];
            medianQuality++;
        }

        return medianQuality;
    }
}
