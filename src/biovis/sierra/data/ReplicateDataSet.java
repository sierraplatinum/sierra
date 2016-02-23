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

import biovis.sierra.data.peakcaller.TagCountHistogram;
import biovis.sierra.data.windows.WindowList;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.math3.distribution.PoissonDistribution;

/**
 *
 * @author Daniel Gerighausen, Lydia Mueller, Dirk Zeckzer
 */
public class ReplicateDataSet {

    private String description;
    private int index;

    private double lambda;
    private double lambdaRaw;

    private TagCountHistogram tagCountHistogram;
    private int medianQuality;
    private int upperQualityQuartile;
    private int lowerQualityQuartile;
    private int minQuality;
    private int maxQuality;

    private int tagCount;

    /**
     * Constructor.
     */
    public ReplicateDataSet() {
    }

    /**
     * Compute quality counts.
     *
     * @param samReaderDefaultFactory sam reader default factory
     */
    public void computeQualityCounts(
            final SamReaderFactory samReaderDefaultFactory
    ) {
        int tagCount = 0;

        int[] qualitycounts = new int[41];
        for (int i = 0; i < qualitycounts.length; i++) {
            qualitycounts[i] = 0;
        }
        //total number of qualities
        long totalcounts = 0;
        //count
        try (SamReader sam = samReaderDefaultFactory.open(new File(description))) {
            try (SAMRecordIterator it = sam.iterator()) {
                while (it.hasNext()) {
                    SAMRecord record = it.next();
                    if (record.getReadUnmappedFlag() == false) {
                        tagCount++;
                        byte[] qualities = record.getBaseQualities();
                        for (byte b : qualities) {
                            int qIndex = (int) b;
                            qualitycounts[qIndex]++;
                            totalcounts++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        computeQuality(totalcounts, qualitycounts, tagCount);
    }

    /**
     * Compute median, quartiles, min, max from quality counts
     *
     * @param totalcounts total number of quality counts
     * @param qualitycounts quality count
     * @param tagCount tag count
     */
    public void computeQuality(long totalcounts, int[] qualitycounts, int tagCount) {
        this.tagCount = tagCount;
        //calculate quartiles, min, and max
        int median = (int) (0.5 * totalcounts);
        int lowerQuartile = (int) (0.25 * totalcounts);
        int upperQuartile = (int) (0.75 * totalcounts);
        long curSum = 0;
        for (int i = 0; i < qualitycounts.length; i++) {
            if (curSum < lowerQuartile && lowerQuartile <= (curSum + qualitycounts[i])) {
                setLowerQualityQuartile(i);
            }
            if (curSum < median && median <= (curSum + qualitycounts[i])) {
                setMedianQuality(i);
            }
            if (curSum < upperQuartile && upperQuartile <= (curSum + qualitycounts[i])) {
                setUpperQualityQuartile(i);
            }
            curSum += qualitycounts[i];
        }

        int min = 0;
        while (min < qualitycounts.length && qualitycounts[min] == 0) {
            min++;
        }
        setMinQuality(min);

        int max = qualitycounts.length - 1;
        while (max >= 0 && qualitycounts[max] == 0) {
            max--;
        }
        setMaxQuality(max);
    }

    /**
     * create tag count histogram from window list
     *
     * @param wl window list
     */
    public void computeTagCountHistogram(
            WindowList wl
    ) {
        tagCountHistogram = new TagCountHistogram(wl, index);
    }

    /**
     * Compute least squares distance for given Poisson distributions.
     *
     * @param poissonsCollection collection of Poisson distributions
     * @return least squares distance
     */
    public double computeLeastSquaresDistance(Map<Integer, PoissonDistribution> poissonsCollection) {
        double leastSquare = 0.0;
        for (int i = 0; i < tagCountHistogram.getMaxCount(); i++) {
            double dist = tagCountHistogram.getCount(i) - poissonsCollection.get(index).probability(i);
            leastSquare += dist * dist;
        }
        leastSquare = Math.sqrt(leastSquare);
        leastSquare /= (tagCountHistogram.getMaxCount() + 1);
        return leastSquare;
    }

    public void computeHistogramBins() {
        tagCountHistogram.toBins();
    }

    /**
     * Get description-
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set description.
     *
     * @param description description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public File getFile() {
        return new File(description);
    }

    /**
     * Get lambda.
     *
     * @return lambda
     */
    public double getLambda() {
        return lambda;
    }

    /**
     * Set lambda.
     *
     * @param lambda lambda
     * @param raw true iff raw lambda is set
     */
    public void setLambda(double lambda, boolean raw) {
        if (raw) {
            this.lambdaRaw = lambda;
        } else {
            this.lambda = lambda;
        }
    }

    /**
     * Get raw lambda-
     *
     * @return raw lambda
     */
    public double getLambdaRaw() {
        return lambdaRaw;
    }

    /**
     * Get tag count histogram.
     *
     * @return tag count histogram
     */
    public TagCountHistogram getHistogram() {
        return tagCountHistogram;
    }

    /**
     * Set tag count histogram
     *
     * @param histogram tag count histogram
     */
    public void setHistogram(TagCountHistogram histogram) {
        this.tagCountHistogram = histogram;
    }

    /**
     * Get index.
     *
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Set index.
     *
     * @param index index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    public int getMedianQuality() {
        return medianQuality;
    }

    public void setMedianQuality(int medianQuality) {
        this.medianQuality = medianQuality;
    }

    public int getUpperQualityQuartile() {
        return upperQualityQuartile;
    }

    public void setUpperQualityQuartile(int upperQualityQuartile) {
        this.upperQualityQuartile = upperQualityQuartile;
    }

    public int getLowerQualityQuartile() {
        return lowerQualityQuartile;
    }

    public void setLowerQualityQuartile(int lowerQuartileQuartile) {
        this.lowerQualityQuartile = lowerQuartileQuartile;
    }

    public int getMinQuality() {
        return minQuality;
    }

    public void setMinQuality(int minQuality) {
        this.minQuality = minQuality;
    }

    public int getMaxQuality() {
        return maxQuality;
    }

    public void setMaxQuality(int maxQuality) {
        this.maxQuality = maxQuality;
    }

    public int getTagCount() {
        return tagCount;
    }

    @Override
    public String toString() {
        return "ReplicateDataSet ["
               + ", index = " + index
               + ", lambda = " + lambda
               + ", lambdaRaw = " + lambdaRaw
               + ", histogram = " + tagCountHistogram
               + ", medianQuality = " + medianQuality
               + ", upperQualityQuartile = " + upperQualityQuartile
               + ", lowerQuartileQuartile = " + lowerQualityQuartile
               + ", minQuality = " + minQuality
               + ", maxQuality = " + maxQuality
               + "]";
    }
}
