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
package biovis.sierra.data;

import biovis.sierra.data.peakcaller.INMReplicate;
import biovis.sierra.data.windows.WindowList;

import htsjdk.samtools.SamReaderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.PoissonDistribution;
import parallel4.Tuple;

/**
 *
 * @author Daniel Gerighausen, Lydia Mueller, Dirk Zeckzer
 */
public class Replicate
  implements INMReplicate {
    private ReplicateDataSet experiment;
    private ReplicateDataSet background;
    private String name;

    private List<Double> globalLambda = new ArrayList<>();
    private int index;
    private String tag;
    private double scalingFactor;
    private double leastSquareDist;

    //final p-value histogram and overlap statistic
    private List<Tuple<Integer, Integer>> finalPValueExp;
    private int maxPValueExp = -1;

    private int significantWindows;
    private Map<String, Integer> significantWindowsChrWise;
    private double weight;
    private boolean active = true;

    /**
     * Constructor.
     */
    public Replicate(String name) {
        scalingFactor = 1.0;
        leastSquareDist = 1.0;
        weight = 1.0;
        experiment = new ReplicateDataSet();
        background = new ReplicateDataSet();
        this.setName(name);
    }

    public Map<String, Integer> getSignificantWindowsChrWise() {
        return significantWindowsChrWise;
    }

    public void setSignificantWindowsChrWise(
            Map<String, Integer> significantWindowsChrWise) {
        this.significantWindowsChrWise = significantWindowsChrWise;
    }

    /**
     * Compute quality counts.
     * @param samReaderDefaultFactory 
     */
    public void computeQualityCounts(final SamReaderFactory samReaderDefaultFactory) {
        // Experiment
        experiment.computeQualityCounts(samReaderDefaultFactory);
        // Background
        background.computeQualityCounts(samReaderDefaultFactory);
    }

    /**
     * create tag count histogram from window list
     *
     * @param wl window list
     */
    public void computeTagCountHistogram(
            WindowList wl
    ) {
        experiment.computeTagCountHistogram(wl);
        background.computeTagCountHistogram(wl);
    }

    /**
     * Compute least squares distance.
     *
     * @param poissonsCollection collection of Poisson distributions
     */
    public void computeLeastSquaresDistance(
            Map<Integer, PoissonDistribution> poissonsCollection
    ) {
        double bgrLeastSquare = background.computeLeastSquaresDistance(poissonsCollection);
        double expLeastSquare = experiment.computeLeastSquaresDistance(poissonsCollection);
        leastSquareDist = bgrLeastSquare + expLeastSquare;
    }

    /**
     * Compute weight.
     */
    public void computeWeight() {
        weight = 1.0 / (1.0 + leastSquareDist);
    }

    /**
     * Compute histogram bins.
     */
    public void computeHistogramBins() {
        experiment.computeHistogramBins();
        background.computeHistogramBins();
    }

    /**
     * Compute scaling factor from tag counts.
     */
    public void computeScalingFactor() {
        int expTagCount = experiment.getTagCount();
        int controlTagCount = background.getTagCount();
        scalingFactor = (double) controlTagCount / (double) expTagCount;
    }

    public List<Double> getGlobalLambda() {
        return globalLambda;
    }

    public void addGlobalLambdaValue(Double value) {
        globalLambda.add(value);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "Replicate ["
               + "experiment = " + experiment.toString()
               + ", background = " + background.toString()
               + ", globalLambda=" + globalLambda
               + ", tag=" + tag
               + ", scalingFactor=" + scalingFactor
               + ", leastSquareDist=" + leastSquareDist
               + ", significantWindows=" + significantWindows
               + ", weight=" + weight
               + ", active=" + active
               + "]";
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public double getLeastSquareDist() {
        return leastSquareDist;
    }

    public void setLeastSquareDist(double leastSquareDist) {
        this.leastSquareDist = leastSquareDist;
    }

        public int getMaxPValueExp() {
        return maxPValueExp;
    }

    public void setMaxPValueExp(int maxPValueExp) {
        if (maxPValueExp > this.maxPValueExp) {
            this.maxPValueExp = maxPValueExp;
        }
    }

    public List<Tuple<Integer, Integer>> getFinalPValueExp() {
        return finalPValueExp;
    }

    public void setFinalPValueExp(List<Tuple<Integer, Integer>> finalPValueExp) {
        this.finalPValueExp = finalPValueExp;
    }

    public int getSignificantWindows() {
        return significantWindows;
    }

    public void setSignificantWindows(int significantWindows) {
        this.significantWindows = significantWindows;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ReplicateDataSet getExperiment() {
        return experiment;
    }

    public void setExperiment(ReplicateDataSet experiment) {
        this.experiment = experiment;
    }

    public ReplicateDataSet getBackground() {
        return background;
    }

    public void setBackground(ReplicateDataSet background) {
        this.background = background;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
