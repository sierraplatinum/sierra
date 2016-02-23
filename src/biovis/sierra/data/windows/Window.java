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
 *****************************************************************************
 */
package biovis.sierra.data.windows;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.SamRecordIntervalIteratorFactory;

import biovis.sierra.data.Replicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import biovis.sierra.server.correlation.CorrelationItem;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class Window
  implements CorrelationItem {

    //Interval containing genomic location
    private String chr;
    private int start;
    private int end;

    //Iterator Factory -- required to get an iterator of overlapping reads for a given interval
    private static SamRecordIntervalIteratorFactory samRecordIntervalIteratorFactory = new SamRecordIntervalIteratorFactory();

    //tag counts -- as double since some of the tag counts have to be normalized
    private double[] tags;

    // raw p-values for single peak calls
    private double[] rawPValues;

    // combined raw p-value
    private double finalRawPValue;

    //p-values for single peak calls
    private double[] pValues;

    //combined p-value
    private double finalPValue;

    /**
     * window object for given genomic location
     * @param chr chromosome on which window is located
     * @param start start position of window
     * @param end end position of window
     * @param numberOfTags number of tags
     * @param numberOfReplicates number of replicates
     */
    public Window(
      String chr,
      int start,
      int end,
      int numberOfTags,
      int numberOfReplicates
    ) {
        //make interval and arraylist of intervals for tag counting
        this.chr = chr;
        this.start = start;
        this.end = end;

        //vector with tag counts
        tags = new double[numberOfTags];
        for (int i = 0; i < numberOfTags; i++) {
            tags[i] = 0.0;
        }

        //pvalues
        rawPValues = new double[numberOfReplicates];
        pValues = new double[numberOfReplicates];
        for (int i = 0; i < numberOfReplicates; i++) {
            pValues[i] = 1.0;//by default not significant at all
            rawPValues[i] = 1.0;//by default not significant at all
        }
        finalPValue = 1.0; //by default not significant at all
        finalRawPValue = 1.0;
    }
    
    /**
     * initialize pValue array after state import with correct size
     */
    
    public void initAfterImport()
    {
    	 pValues = new double[rawPValues.length];
    	    for (int i = 0; i < pValues.length; i++) {
                pValues[i] = 1.0;//by default not significant at all
             
            }
    }

    /**
     * count tags for a  given data set using the given sam reader
     *
     * @param dataset data set which should be counted
     * @param sam sam reader used to get sam records
     */
    public void count(Integer dataset, SamReader sam) {
        //count
        List<Interval> intervalList = new ArrayList<>();
        intervalList.add(new Interval(chr, start, end));
        double tagsNum;
        try (CloseableIterator<SAMRecord> overlapping = samRecordIntervalIteratorFactory.makeSamRecordIntervalIterator(sam, intervalList, true)) {
            tagsNum = 0.0;
            while (overlapping.hasNext()) {
                tagsNum++;
                overlapping.next();
            }
        }

        tags[dataset] = tagsNum;
    }

    /**
     * scale tag counts of all experiment with their scaling factors
     *
     * @param replicates list of replicates
     */
    public void scale(
      List<Replicate> replicates
    ) {
        for (Replicate replicate : replicates) {
            //scale
            int index = replicate.getExperiment().getIndex();
            tags[index] *= replicate.getScalingFactor();
        }
    }

    /**
     * Get start position
     *
     * @return start position
     */
    public int getStart() {
        return start;
    }

    /**
     * Get end position
     *
     * @return end position
     */
    public int getEnd() {
        return end;
    }

    /**
     * Get chromosome
     *
     * @return chromosome
     */
    public String getChr() {
        return chr;
    }

    /**
     * return tag count for data set
     * @param dataset data set requested
     * @return tag counts
     */
    public double getTagCount(int dataset) {
        return tags[dataset];
    }

    /**
     * set tag count for data set
     * @param dataset data set
     * @param tagCount tag counts
     */
    public void setTagCount(int dataset, double tagCount) {
        tags[dataset] = tagCount;
    }

    /**
     * sums up all tag counts and returns this value
     * @return sum of tag counts
     */
    public double getTagSum() {
        double sum = 0.0;
        for (double tag : tags) {
            sum += tag;
        }
        return sum;
    }

    /**
     * set the pvalue of data set to given p-value
     * @param dataset dataset to change
     * @param val new p-value
     */
    public void setRawPValue(int dataset, double val) {
        rawPValues[dataset] = val;
    }

    /**
     * get all p-values of single peak calls
     * @return double array with p-values
     */
    public double[] getRawPValueList() {
        return pValues;
    }

    /**
     * get combined p-value
     * @return double representing the final p-value
     */
    public double getFinalRawPValue() {
        return finalRawPValue;
    }

    /**
     * setter for the final p-value
     * @param finalPValue new final p-value
     */
    public void setFinalRawPValue(double finalRawPValue) {
        this.finalRawPValue = finalRawPValue;
    }

    /**
     * set the pvalue of data set to given p-value
     * @param dataset dataset to change
     * @param val new p-value
     */
    public void setPValue(int dataset, double val) {
        pValues[dataset] = val;
    }

    /**
     * get all p-values of single peak calls
     * @return double array with p-values
     */
    public double[] getPValueList() {
        return pValues;
    }

    /**
     *
     * @return
     */
    @Override
    public double [] getCorrelationValues() {
        return getPValueList();
    }
    
    /**
     * get combined p-value
     * @return double representing the final p-value
     */
    public double getFinalPValue() {
        return finalPValue;
    }

    /**
     * setter for the final p-value
     * @param finalPValue new final p-value
     */
    public void setFinalPValue(double finalPValue) {
        this.finalPValue = finalPValue;
    }

    /**
     * print all tag counts to stderr for debugging purposes
     */
    public void printTagCounts() {
        for (int i = 0; i < tags.length; i++) {
            System.err.println(tags[i]);
        }
    }

    /**
     * Clear information of window that does not need to be exported
     */
    public void installLinux() {
        // remove tags; only p values are exported
        tags = null;
        pValues = null;
        finalPValue = 0;
    }

    @Override
    public String toString() {
        return "Window ["
               + "chr=" + chr
               + ", start=" + start
               + ", end=" + end
               + ", tags=" + Arrays.toString(tags)
               + ", pValues=" + Arrays.toString(pValues)
               + ", finalPValue=" + finalPValue
               + "]";
    }
    
    public double generalGetRawPValue(int replicate){
    	if(replicate >= this.rawPValues.length) return finalRawPValue;
    	return rawPValues[replicate];
    }

    public void generalSetPValue(double pval, int replicate){
    	if(replicate >= this.pValues.length)finalPValue  = pval;
    	else pValues[replicate] = pval;
    }
}
