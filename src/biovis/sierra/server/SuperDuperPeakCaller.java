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
package biovis.sierra.server;

import biovis.sierra.server.correlation.PearsonCorrelationParallelFast;
import biovis.sierra.server.p2q.QValueCalculator;
import biovis.sierra.server.p2q.QValueCalculatorHB;
import biovis.sierra.server.p2q.QValueCalculatorStoreyBootStrapSpline;
import biovis.sierra.server.p2q.QValueCalculatorStoreySimple;
import biovis.sierra.server.peakMe.PeakMeSpaceParallel;
import biovis.sierra.server.peakQuality.SuperDuperQualityCoherentBN3Smart;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.PeakList;
import biovis.sierra.data.peakcaller.PeakQuality;
import biovis.sierra.data.peakcaller.PoissonDistributionCollection;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.server.Commander.PeakCommander;
import biovis.sierra.server.p2q.QValueCalculator;
import biovis.sierra.server.windowFactories.WindowFactoryReaderSerial1ChunkParallelCoherent2;

import htsjdk.samtools.SamReaderFactory;

import parallel4.Parallel;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import parallel4.IterationInt;
import parallel4.IterationParameter;
import parallel4.Parallel2;
import parallel4.ParallelForParameter;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class SuperDuperPeakCaller {

    private DataMapper mapper;

    private Integer windowSize;
    private Integer windowOffset;

    private WindowList wl;
    private int chunkSize = 10000;

    // old lists; new lists are in peak factory
    private PeakList narrowList = null;
    private PeakList broadList = null;

    private PeakCommander pc;

    private PoissonDistributionCollection pdc;

    /**
     * Constructor.
     *
     * @param dm
     *            data mapper
     * @param pc
     *            peak commander
     */
    public SuperDuperPeakCaller(DataMapper dm, PeakCommander pc, int chunkSize) {
        this.mapper = dm;
        windowSize = mapper.getWindowsize();
        windowOffset = mapper.getOffset();
        this.chunkSize = chunkSize;

        Parallel.adjustCorePool(mapper.getNumCores());

        this.pc = pc;
    }

    /**
     * Constructor.
     *
     * @param dm
     *            data mapper
     */
    public SuperDuperPeakCaller(DataMapper dm, int chunkSize) {
        this(dm, null, chunkSize);
    }

    /**
     * Initialization after loading a data mapper. !Data mapper has to be loaded
     * before (includes window list)!
     */
    public void init() {
        Logger log = Logger.getLogger("SuperDuperPeakCaller.init");
        log.info("init peak caller");

        log.info("creating new peak factory");
        PeakFactory pf = new PeakFactory(wl, mapper.getWindowsize());
        pf.setThreshold(mapper.getPvaluecutoff());
        log.info("peak factory created");

        /*
        log.info("establish peak quality");
        SuperDuperQuality broadQuality = new SuperDuperQuality(mapper, wl,
                                                               broadList, pf.getPeakListBroad());
        mapper.setBroadPeakQuality(broadQuality.evaluatePeakList());
        SuperDuperQuality narrowQuality = new SuperDuperQuality(mapper, wl,
                                                                narrowList, pf.getPeakListNarrow());
        mapper.setNarrowPeakQuality(narrowQuality.evaluatePeakList());

        broadList = new PeakList(pf.getPeakListBroad());
        narrowList = new PeakList(pf.getPeakListNarrow());
        log.info("peak quality established");
         */
        log.info("init done");
    }

    /**
     *
     * @param message
     */
    private void timestamp(String message) {
        System.err.println("TimeStamp: " + message + ","
                           + LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
    }

    /**
     * Start the peak calling process. Includes two stages: - stage 1: compute
     * peak calls per replicate - stage 2: combine peak calls
     *
     * To change the computation of the window construction, change the line wl
     * = ... replacing the WindowFactoryClass
     *
     * !for non-coherent methods, 'estimate quality mappings' has to be
     * included!
     *
     * Alternatives for the computation of the final peak quality are available
     *
     * @return PeakFactory
     * @throws IOException
     */
    public PeakFactory start() throws IOException {

        // log parameters
        Logger log = Logger.getLogger("SuperDuperPeakCaller.start");
        log.info("peak calling started");
        mapper.setCurrentStep(1);
        log.log(Level.INFO, "windowSize: {0}", windowSize);
        log.log(Level.INFO, "offset: {0}", windowOffset);
        log.log(Level.INFO, "p-value: {0}", mapper.getPvaluecutoff());
        log.log(Level.INFO, "cores: {0}", mapper.getNumCores());
        log.info("constructing windows [very IO intensiv]");
        timestamp("Start");

        mapper.finalizeReplicateList();

        // make windows and count tags for windows
        // Change this to test the window constructions
        // A:
        // wl = WindowFactoryCountTagParallel.constructWindows(mapper, windowSize, windowOffset, pc);
        // B:
        // wl = WindowFactoryChromosomeParallel.constructWindows(mapper, windowSize, windowOffset, pc);
        // C: Chunk - Window - Dataset
        // wl = WindowFactoryChunkParallelWindowDataset2.constructWindows(mapper, windowSize, windowOffset, pc);
        // D: Chunk - Dataset - Window
        // wl = WindowFactoryChunkParallelDataSetWindow.constructWindows(mapper, windowSize, windowOffset, pc);
        // E-1 -- F-2: Dataset - Chunk - Window
        // E-1: (fastest)
        // wl = WindowFactoryReaderSerial2ChunkParallelCoherent.constructWindows(mapper, windowSize, windowOffset, pc);
        // E-2: (fastest)
        // wl = WindowFactoryReaderSerial2ChunkParallelCoherent2.constructWindows(mapper, windowSize, windowOffset, pc);
        // E-3: (fastest, space efficient)
        wl = WindowFactoryReaderSerial1ChunkParallelCoherent2.constructWindows(mapper, windowSize, windowOffset, pc, chunkSize);
        // F-1:
        // wl = WindowFactoryReaderSerial1ChunkParallel.constructWindows(mapper, windowSize, windowOffset, pc);
        // F-2:
        // wl = WindowFactoryReaderSerial2ChunkParallel.constructWindows(mapper, windowSize, windowOffset, pc);

        // For debugging purposes: print window list
        // printWindows(wl);
        log.info("window list constructed");
        setProgress(0.25);
        timestamp("Constructing windows");

        // Estimate mapping quality
        log.info("estimating mapping quality [very IO intensiv]");
        /*
        // for coherent window construction: included there
        // needs to be activated for non coherent window list construction
        estimateMappingQuality();
        */
        log.info("mapping quality estimated");
        setProgress(0.30);
        timestamp("Mapping Quality for all Replicates");

        // generate poisson distributions for data
        log.info("estimating raw poisson distribution");
        pdc = new PoissonDistributionCollection(mapper);
        pdc.estimateLambdas(wl, true);
        log.info("raw poisson distribution estimated");
        setProgress(0.35);
        timestamp("Raw Poisson Distributions for all Replicates");

        // Compute tag count histogram and least square weight
        log.info("compute tag count histograms and least square weight");
        List<Replicate> replicates = mapper.getReplicates();
        Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
        new ParallelForParameter<>(p2, replicates).loop(
                new IterationParameter<Replicate>() {
            @Override
            public void iteration(Replicate replicate) {
                replicate.computeTagCountHistogram(wl);
                replicate.computeLeastSquaresDistance(pdc.getPoissonsCollection());
                replicate.computeHistogramBins();

                replicate.computeWeight();
            }
        });
        log.info("tag count histograms generated");
        setProgress(0.40);
        timestamp("Tag Count Distribution / Least Square for all Replicates");

        // estimate scaling factors
        log.info("estimate scaling factors and scale experiments");
        estimateScalingFactors();
        wl.scaleAllExperiments();
        log.info("experiments scaled");
        setProgress(0.45);
        timestamp("Scaling library for all replicates");

        // estimate normalized lambdas
        log.info("estimating normalized poisson distribution");
        pdc.estimateLambdas(wl, false);
        log.info("normalized poisson distribution estimated");
        setProgress(0.50);
        timestamp("Normalized Poisson distribution for all replicates");

        // generate experiment-wise p-values
        log.info("generating peak calls for single replicates");
        {
            // PeakMe peakMe = new PeakMe(wl, pdc, mapper);
            PeakMeSpaceParallel peakMe = new PeakMeSpaceParallel(wl, pdc, mapper);
            peakMe.peak();
            peakMe = null;

            // System.gc();
        }
        log.info("single peak calls generated");
        timestamp("Single peak calls for all replicates");

        calculateQValues(log);

        calculateCorrelations(log);

        // compute the combined replicates
        PeakFactory pf = combinedReplicatesPart(log);

        return pf;
    }

    /**
     * Calculate p-values, transform them into q-values, and compute quality measures.
     *
     * @param log logger
     */
    private void calculateQValues(Logger log) {
        log.info("compute q-values for single peaks");
        {
            // create q value calculator instance and sorted index
            // sorting is done in parallel

            QValueCalculator[] qValCalc = new QValueCalculator[mapper.getReplicates().size()];
            for (int replicate = 0; replicate < qValCalc.length; ++replicate) {
              qValCalc[replicate] = getQValueCalculator();
              double[] values = getGeneralRawPValues(wl.getWindows(), replicate);
              qValCalc[replicate].createSortedIndex(values);
            }

            // compute q-value correction in parallel
            Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
            new ParallelForInt2(p2, 0, mapper.getReplicates().size()).loop(
                    new IterationInt() {

                @Override
                public void iteration(int index) {

                    log.log(Level.INFO,
                            "compute q-values for single peaks {0}",
                            index);
                    double[] qValues = qValCalc[index].p2q();
                    setGeneralPValues(wl.getWindows(), index, qValues);
                    log.log(Level.INFO,
                            "q-values for single peaks computed {0}",
                            index);
                }
            });
        }
        log.info("q-values for single peaks computed");
        setProgress(0.55);
        timestamp("Transform Single Replicate p- to q-Values");

        // estimate single replicate quality
        log.info("estimate single replicate quality: significant window distribution");
        SignificantWindowsOptimized.estimateDistanceToMedianOfSignificantWindows(mapper, wl);
        log.info("single replicate quality: significant window distribution estimated");
        timestamp("Significant windows distribution for all replicates");

        log.info("estimate single replicate quality: p-value distribution");
        PValueDistribution.estimatePValueDistribution(mapper, wl);
        log.info("single replicate quality: p-value distribution estimated");
        setProgress(0.60);
        timestamp("Significant p-value distribution for all replicates");
    }

    /**
     * Create instance for calculating q-values from datamapper setting.
     *
     */
    private QValueCalculator getQValueCalculator() {
        //
        switch (mapper.getQValueMethod()) {
            case "Holm Bonferroni":
                return new QValueCalculatorHB(mapper.getNumCores());

            case "Storey Simple":
                return new QValueCalculatorStoreySimple(mapper.getNumCores());

            case "Storey Bootstrap":
                return new QValueCalculatorStoreyBootStrapSpline(mapper.getNumCores());

            default:
                return new QValueCalculatorHB(mapper.getNumCores());
        }
    }

    /**
     * Calculate replicate correlations.
     *
     * @param log logger
     */
    private void calculateCorrelations(Logger log) {
        // estimate correlation between the replicates
        log.info("estimating replicate correlation");
        {
            // PearsonCorrelation pCorr = new PearsonCorrelation(mapper.getReplicates().size(), mapper.getNumCores());
            // PearsonCorrelationParallel pCorr = new PearsonCorrelationParallel(mapper.getReplicates().size(), mapper.getNumCores());
            PearsonCorrelationParallelFast pCorr = new PearsonCorrelationParallelFast(mapper.getReplicates().size(), mapper.getNumCores());
            double [][] corr = pCorr.calculateCorrelations(wl);
            mapper.setReplicatePearsonCorrelation(corr);
        }
        log.info("replicate correlation calculated");
        setProgress(0.65);
        timestamp("Correlation between all replicates");
    }

    /**
     * compute combined replicates. this part might be executed several times
     * with changing weights.
     *
     * @param log
     *            logger
     * @return peak factory
     */
    private PeakFactory combinedReplicatesPart(Logger log) {
        // generate consensus peaks
        log.info("generating multi-replicate peak calls");
        {
            PeakUs peakUs = new PeakUs(mapper);
            peakUs.peak(wl);
        }
        log.info("multi-replicate peak calls generated");
        timestamp("Multi Peak Calls");

        log.info("generating q-values for multi-replicate peak calls");
        {
            // create q value calculator instance
            QValueCalculator qValCalc = getQValueCalculator();
            double[] pValues = getGeneralRawPValues(wl.getWindows(), mapper.getReplicates().size());
            qValCalc.createSortedIndex(pValues);

            // compute q-value correction
            double[] qValues = qValCalc.p2q();
            setGeneralPValues(wl.getWindows(), mapper.getReplicates().size(), qValues);
        }
        log.info("q-values for multi-replicate peak calls generated");
        setProgress(0.70);
        timestamp("Transform Multi Replicate p- to q-Values");

        // generate final p-values
        log.info("generating p-value distribution for final p-values");
        PValueDistribution.estimateFinalPValueDistribution(mapper, wl);
        log.info("p-value distribution for final p-values generated");
        setProgress(0.75);
        timestamp("Estimating p-value distribution of final p-values");

        // estimate overlap of replicate with final results (amount of peaks)
        log.info("generating replicate overlap with final results");
        OverlapRatio.estimateOverlapRatio(mapper, wl);
        log.info("replicate overlap with final results generated");
        setProgress(0.80);
        timestamp("Compute replicate overlap with final result");

        // create peak factory and peaks
        PeakFactory pf = new PeakFactory(wl, mapper.getWindowsize());
        pf.setThreshold(mapper.getPvaluecutoff());
        log.info("peak factory created");

        log.info("creating narrow peaks");
        narrowList = pf.getPeakListNarrow();
        log.info("narrow peaks created");

        log.info("creating broad peaks");
        broadList = pf.getPeakListBroad();
        log.info("broad peaks created");
        setProgress(0.85);
        timestamp("Create Peak Factor and Narrow Peaks and Broad Peaks");

        // determine peak qualities
        if (mapper.isQualityCounting()) {
            log.info("establish peak quality");

            /*
            //System.err.println("Broad");
            // Peak parallel
            //SuperDuperQuality broadQuality = new SuperDuperQuality(
            // Peak coherent parallel
            //SuperDuperQualityCoherent broadQuality = new SuperDuperQualityCoherent(
            // Peak coherent parallel - space
            //SuperDuperQualityCoherentSpace broadQuality = new SuperDuperQualityCoherentSpace(
            // Peak coherent parallel - space - smart
            SuperDuperQualityCoherentSpaceSmart broadQuality = new SuperDuperQualityCoherentSpaceSmart(
               mapper, wl, null, broadList);
             mapper.setBroadPeakQuality(broadQuality.evaluatePeakList());
             broadList = new PeakList(broadList);

            //System.err.println("Narrow");
            //SuperDuperQuality narrowQuality = new SuperDuperQuality(
            //SuperDuperQualityCoherent narrowQuality = new SuperDuperQualityCoherent(
            //SuperDuperQualityCoherentSpace narrowQuality = new SuperDuperQualityCoherentSpace(
            SuperDuperQualityCoherentSpaceSmart narrowQuality = new SuperDuperQualityCoherentSpaceSmart(
               mapper, wl, null, narrowList);
             mapper.setNarrowPeakQuality(narrowQuality.evaluatePeakList());
             narrowList = new PeakList(narrowList);
            */

            //SuperDuperQualityCoherentBN sdqCoherentBN = new SuperDuperQualityCoherentBN(
            //SuperDuperQualityCoherentBN2Smart sdqCoherentBN = new SuperDuperQualityCoherentBN2Smart(
            SuperDuperQualityCoherentBN3Smart sdqCoherentBN = new SuperDuperQualityCoherentBN3Smart(
                    mapper, wl, broadList, narrowList);
            PeakQuality pqNarrow = new PeakQuality();
            PeakQuality pqBroad = new PeakQuality();
            sdqCoherentBN.evaluatePeakList(pqBroad, pqNarrow);
            mapper.setNarrowPeakQuality(pqNarrow);
            mapper.setBroadPeakQuality(pqBroad);
            narrowList = new PeakList(narrowList);
            broadList = new PeakList(broadList);

            // System.gc();

            log.info("peak quality established");
        }
        setProgress(0.90);
        timestamp("Establish Peak Quality");
        return pf;
    }

    /**
     * @param windows
     * @param replicate 
     * @return general raw p values for replicate
     */
    private double[] getGeneralRawPValues(
            List<Window> windows,
            int replicate
    ) {
        double[] values = new double[windows.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = windows.get(i).generalGetRawPValue(replicate);
        }
        return values;
    }

    /**
     * @param windows
     * @param replicate
     * @param qValues
     */
    private void setGeneralPValues(
            List<Window> windows,
            int replicate,
            double[] qValues
    ) {
        for (int i = 0; i < qValues.length; i++) {
            windows.get(i).generalSetPValue(qValues[i], replicate);
        }
    }

    /**
     * Restart multi-peak-calling.
     *
     * @param mapper
     *            mapper
     * @return peak factory
     */
    public PeakFactory restart(DataMapper mapper) {
        Logger log = Logger.getLogger("SuperDuperPeakCaller.restart");
        mapper.setCurrentStep(mapper.getCurrentStep() + 1);
        mapper.finalizeReplicateList();

        log.info("Restarting Multi Peak Calling");
        try {
            this.mapper = mapper;
            wl.setDataMapper(mapper);
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.log(Level.INFO,
                "Restarting multi peak-calling: correcting for correlation={0} weighted={1}",
                new Object[]{mapper.isCorrectCorrelation(), mapper.isWeighted()});
        /*
        for (int i = 0; i < mapper.getReplicates().size(); i++) {
            Replicate r = mapper.getReplicates().get(i);
            if (r.isActive()) {
                 log.log(Level.INFO, "Replicate {0}: used", (i + 1));
             } else {
                 log.log(Level.INFO, "Replicate {0}: not used", (i + 1));
             }
        }
         */
        calculateQValues(log);

        calculateCorrelations(log);

        // compute the combined replicates
        PeakFactory pf = combinedReplicatesPart(log);

        return pf;
    }

    /**
     * Compute Phread score values.
     *
     * @throws IOException
     *             in case of IO problems
     */
    private void estimateMappingQuality() throws IOException {
        // Parralelization does not reduce computation time
        /*
         * Parallel2 parallel = new
         * Parallel2(mapper.getNumCoresMappingQuality()); new
         * ParallelForInt2(parallel, 0, mapper.getReplicates().size()).loop(new
         * IterationInt() {
         *
         * @Override public void iteration(int index) { final SamReaderFactory
         * samReaderDefaultFactory = SamReaderFactory.makeDefault();
         * mapper.getReplicates().get(index).computeQualityCounts(samReaderDefaultFactory); } });
         */

        final SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();
        for (Replicate r : mapper.getReplicates()) {
            r.computeQualityCounts(samReaderDefaultFactory);
        }
        // System.gc();
    }

    /**
     * Estimate the scaling factors for all experiments.
     *
     * @throws IOException
     */
    private void estimateScalingFactors() throws IOException {
        for (Replicate r : mapper.getReplicates()) {
            r.computeScalingFactor();
        }
    }

    /**
     * Print windows in window list for debugging purposes.
     *
     * @param wl
     *            window list
     */
    private void printWindows(WindowList wl) {
        int j = 0;
        for (Window w : wl.getWindows()) {
            System.err.println("+++++++++++Window " + j + "+++++++++++");
            System.err.println(w.getChr() + ":" + w.getStart() + "-"
                               + w.getEnd());
            w.printTagCounts();
            ++j;
        }
    }

    /**
     * set progress
     *
     * @param progress
     *            progress
     */
    private void setProgress(double progress) {
        if (pc != null) {
            Object[] command = new Object[2];
            command[0] = "setProgress";
            command[1] = progress;
            pc.sendCommand(command);
        }
    }

    /**
     * Get data mapper
     *
     * @return data mapper
     */
    public DataMapper getMapper() {
        return mapper;
    }

    /**
     * Get window list.
     *
     * @return window list
     */
    public WindowList getWl() {
        return wl;
    }

    /**
     * Set window list-
     *
     * @param wl
     *            window list
     */
    public void setWl(WindowList wl) {
        this.wl = wl;
    }
}
