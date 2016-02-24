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

import biovis.sierra.data.peakcaller.PeakQuality;
import biovislib.parallel4.Tuple;
import biovislib.statistics.INMReplicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Daniel Gerighausen, Lydia Mueller, Dirk Zeckzer
 */
public class DataMapper {

    // Input data
    private List<Replicate> replicates;
    private transient Map<Integer, ReplicateDataSet> replicateDataSetByTag;

    //Parameters
    private int windowsize = 200;
    private int offset = 50;
    private double pvaluecutoff = 1E-5;
    private boolean weighted;
    private boolean correctCorrelation;
    private boolean qualityCounting;

    private boolean results = false;

    // defaults for parallelization
    private int numCores = 4;
    private int numCoresWindowFactory = 6;
    private int numCoresPeakQuality = 6;

    // Results
    private Map<String, Integer> significantWindowMedianChrWise;

    private double[][] replicatePearsonCorrelation;

    private PeakQuality broadPeakQuality;
    private PeakQuality narrowPeakQuality;

    // job name and current step performed for this job
    private String jobName;
    private int currentStep = 0;

    //final p-value histogram and overlap statistics per step
    private Map<Integer, List<Tuple<Integer, Integer>>> finalPValueExp = new HashMap<>();
    private List<Tuple<Integer, Integer>> pValueMedianExp = null;
    private int maxPValueExp = -1;

    private Map<Integer, Map<Integer, Double>> overlapWithReplicates = new HashMap<>();
    private String qValueMethod = "Holm Bonferroni";
    //private String qValueMethod = "Storey Simple";
    //private String qValueMethod = "Storey Bootstrap";

    /**
     * Constructor.
     */
    public DataMapper() {
        replicates = new ArrayList<>();
        replicateDataSetByTag = new HashMap<>();

        setWeighted(true);
        setCorrectCorrelation(true);
        setQualityCounting(true);
    }

    public int getNumberOfDataSets() {
        if (replicateDataSetByTag.isEmpty()) {
            finalizeReplicateList();
        }
        return replicateDataSetByTag.size();
    }

    public ReplicateDataSet getDataSetByTag(int dataSetIndex) {
        if (replicateDataSetByTag.isEmpty()) {
            finalizeReplicateList();
        }
        return replicateDataSetByTag.get(dataSetIndex);
    }

    public void addReplicate(Replicate rep) {
        replicates.add(rep);
    }

    public List<Replicate> getReplicates() {
        return replicates;
    }
    
    public List<? extends INMReplicate> getINMReplicates() {
        return getReplicates();
    }

    /**
     * Finalize list of replicates.
     */
    public void finalizeReplicateList() {
        replicateDataSetByTag.clear();

        for (int i = 0; i < replicates.size(); i++) {
            replicates.get(i).setIndex(i);
            replicates.get(i).getExperiment().setIndex(2 * i);
            replicateDataSetByTag.put(2 * i, replicates.get(i).getExperiment());
            replicates.get(i).getBackground().setIndex(2 * i + 1);
            replicateDataSetByTag.put(2 * i + 1, replicates.get(i).getBackground());
        }
    }

    public int getWindowsize() {
        return windowsize;
    }

    public void setWindowsize(int windowsize) {
        this.windowsize = windowsize;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public double getPvaluecutoff() {
        return pvaluecutoff;
    }

    public void setPvaluecutoff(double pvaluecutoff) {
        this.pvaluecutoff = pvaluecutoff;
    }

    public double[][] getReplicatePearsonCorrelation() {
        return replicatePearsonCorrelation;
    }

    public void setReplicatePearsonCorrelation(
            double[][] replicatePearsonCorrelation) {
        this.replicatePearsonCorrelation = replicatePearsonCorrelation;
    }

    public int getNumCores() {
        return numCores;
    }

    public int getNumCoresWindowFactory() {
    	System.err.println("NumCoresWindowFactory "+ numCoresWindowFactory);
        return numCoresWindowFactory;
    }

    public int getNumCoresPeakQuality() {
        return numCoresPeakQuality;
    }

    public void setNumCores(int numCores) {
        this.numCores = numCores;
        if (numCoresWindowFactory > numCores) {
            numCoresWindowFactory = numCores;
        }
        if (numCoresPeakQuality > numCores) {
            numCoresPeakQuality = numCores;
        }
    }

    public boolean isWeighted() {
        return weighted;
    }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    public boolean isCorrectCorrelation() {
        return correctCorrelation;
    }

    public void setCorrectCorrelation(boolean correctCorrelation) {
        this.correctCorrelation = correctCorrelation;
    }

    public Map<Integer, List<Tuple<Integer, Integer>>> getFinalPValueExp() {
        return finalPValueExp;
    }

    public void setFinalPValueExp(List<Tuple<Integer, Integer>> finalPValueExp) {
        this.finalPValueExp.put(currentStep, finalPValueExp);
    }

    public List<Tuple<Integer, Integer>> getPValueMedianExponents() {
        return pValueMedianExp;
    }

    public void setPValueMedianExponents(List<Tuple<Integer, Integer>> pValueMedianExp) {
        this.pValueMedianExp = pValueMedianExp;
    }

    /**
     * @return the broadPeakQuality
     */
    public PeakQuality getBroadPeakQuality() {
        return broadPeakQuality;
    }

    /**
     * @param broadPeakQuality the broadPeakQuality to set
     */
    public void setBroadPeakQuality(PeakQuality broadPeakQuality) {
        this.broadPeakQuality = broadPeakQuality;
    }

    /**
     * @return the narrowPeakQuality
     */
    public PeakQuality getNarrowPeakQuality() {
        return narrowPeakQuality;
    }

    /**
     * @param narrowPeakQuality the narrowPeakQuality to set
     */
    public void setNarrowPeakQuality(PeakQuality narrowPeakQuality) {
        this.narrowPeakQuality = narrowPeakQuality;
    }

    /**
     * @return the overlapWithReplicates
     */
    public Map<Integer, Map<Integer, Double>> getOverlapWithReplicates() {
        return overlapWithReplicates;
    }

    /**
     * @param overlapWithReplicates the overlapWithReplicates to set
     */
    public void setOverlapWithReplicates(
            Map<Integer, Double> overlapWithReplicates) {
        this.overlapWithReplicates.put(currentStep, overlapWithReplicates);
    }

    public boolean isQualityCounting() {
        return qualityCounting;
    }

    public void setQualityCounting(boolean qualityCounting) {
        this.qualityCounting = qualityCounting;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Map<String, Integer> getSignifcantWindowMedianChrWise() {
        return significantWindowMedianChrWise;
    }

    public void setSignifcantWindowMedianChrWise(
            Map<String, Integer> signifcantWindowMedianChrWise) {
        this.significantWindowMedianChrWise = signifcantWindowMedianChrWise;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void resetCurrentStep() {
        currentStep = 1;
    }
    
    public void incrementCurrentStep() {
        ++currentStep;
    }

    public int getMaxPValueExp() {
        return maxPValueExp;
    }

    public void setMaxPValueExp(int maxPValueExp) {
        if (maxPValueExp > this.maxPValueExp) {
            this.maxPValueExp = maxPValueExp;
        }
    }

    public boolean hasResults() {
        return results;
    }

    public void setResults(boolean results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "DataMapper [pValueMedianExp=" + pValueMedianExp + "]";
    }

    public void setQValueMethod(String value) {
        qValueMethod = value;
    }

    public String getQValueMethod() {
        return qValueMethod;
    }

    public void setIOThreads(int threads) {
        if (this.numCores < threads) {
            return;
        }
        this.numCoresPeakQuality = threads;
        this.numCoresWindowFactory = threads;
    }
}
