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

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.QualityCounter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.Peak;
import biovis.sierra.data.peakcaller.PeakList;
import biovis.sierra.data.peakcaller.PeakQuality;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import java.util.logging.Level;
import parallel4.IterationInt;
import parallel4.Parallel2;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class SuperDuperQualityCoherent {

    //private final static int CHUNK_SIZE = 1;
    private final static int CHUNK_SIZE = 1000;
    private final static int QUEUE_SIZE = 10000;

    // Comparator for start and end queue
    private static Comparator<SAMRecord> compareSAMRecordByStart = (s1, s2) -> Integer.compare(s1.getStart(), s2.getStart());
    private static Comparator<SAMRecord> compareSAMRecordByEnd = (s1, s2) -> Integer.compare(s1.getEnd(), s2.getEnd());

    private DataMapper dm;
    private WindowList wl;
    private PeakList oldPeakList;
    private PeakList peakList;

    /**
     * Constructor.
     *
     * @param dm data mapper
     * @param wl window list
     * @param oldPeakList old list with peaks
     * @param peakList list with peaks
     */
    public SuperDuperQualityCoherent(
            DataMapper dm,
            WindowList wl,
            PeakList oldPeakList,
            PeakList peakList
    ) {
        this.dm = dm;
        this.wl = wl;
        this.oldPeakList = oldPeakList;
        this.peakList = peakList;
    }

    /**
     * Evaluate peak list.
     *
     * @return peak quality
     */
    public PeakQuality evaluatePeakList() {
        Logger log = Logger.getLogger("starting evaluation of peak list");
        log.info("starting evaluation of peak list");
        if (oldPeakList != null) {
            PeakQuality pq = evaluatePeakListUseExisting(peakList);
            log.info("evaluation of peak list done");
            return pq;
        }

        //log.info("starting evaluation of peak list II");
        PeakQuality pq = new PeakQuality();

        int r_index = 0;
        for (Replicate r : dm.getReplicates()) {
            //log.log(Level.INFO, "starting evaluation of peak list for replicate {0}", r.getExperimentDescription());
            if (!r.isActive()) {
                r_index++;
                continue;
            }
            List<Integer> medianBackgroundQualities = calcMedianPeakQualityAndReplicateQuality(r.getBackground().getDescription(), peakList, r_index, false, true);
            log.log(Level.INFO, "Evaluation of peak list for replicate background {0} done", r.getBackground().getDescription());
            List<Integer> medianExperimentQualities = calcMedianPeakQualityAndReplicateQuality(r.getExperiment().getDescription(), peakList, r_index, true, true);
            log.log(Level.INFO, "Evaluation of peak list for replicate experiment {0} done", r.getExperiment().getDescription());

            pq.addExperimentFor(SuperDuperQualityHelper.makeBoxplotData(medianExperimentQualities), r_index);
            pq.addBackgroundFor(SuperDuperQualityHelper.makeBoxplotData(medianBackgroundQualities), r_index);
            r_index++;
            log.log(Level.INFO, "Evaluation of peak list for replicate {0} done", r.getExperiment().getDescription());
        }

        log.info("evaluation of peak list done");

        return pq;
    }

    /**
     * Evaluate existing peak list.
     *
     * @param pl peak list
     * @return peak quality
     */
    private PeakQuality evaluatePeakListUseExisting(PeakList pl) {
        Logger log = Logger.getLogger("starting evaluation of peak list using existing peak list");
        log.info("starting evaluation of peak list using existing peak list");

        PeakList newPeaks = new PeakList();
        PeakList oldPeaks = new PeakList();

        log.info("creating chromosome ordering");
        //generate chromosome Order
        HashMap<String, Integer> chrOrder = new HashMap<>();
        for (Window w : wl.getWindows()) {
            if (!chrOrder.containsKey(w.getChr())) {
                int index = chrOrder.size();
                chrOrder.put(w.getChr(), index);
            }
        }
        log.info("chromsome order available");

        int oldIndex = 0;
        int newIndex = 0;
        while (oldIndex < oldPeakList.size() && newIndex < pl.size()) {
            if (oldPeakList.get(oldIndex).equals(pl.get(newIndex))) {
                //peaks are the same -> keep old peak with quality
                oldPeaks.addPeak(oldPeakList.get(oldIndex));

                oldIndex++;
                newIndex++;
                continue;
            }
            if (chrOrder.get(oldPeakList.get(oldIndex).getChr()) < chrOrder.get(pl.get(newIndex).getChr())) {
                //chromsome with only old peaks --> discard peaks
                oldIndex++;
                continue;
            }
            if (chrOrder.get(pl.get(newIndex).getChr()) < chrOrder.get(oldPeakList.get(oldIndex).getChr())) {
                //chromosome with only new Peaks --> keep in list for quality assessment
                newPeaks.addPeak(pl.get(newIndex));
                newIndex++;
                continue;
            }
            if (oldPeakList.get(oldIndex).getEnd() < pl.get(newIndex).getStart()) {
                //old peak before next new peak --> discard old peak
                oldIndex++;
                continue;
            }
            if (pl.get(newIndex).getEnd() < oldPeakList.get(oldIndex).getStart()) {
                //new peak before next old peak --> collect for quality assessment
                newPeaks.addPeak(pl.get(newIndex));
                newIndex++;
                continue;
            }
            //old and new peak are overlapping --> discard old peak and keep new peak for quality assessment
            newPeaks.addPeak(pl.get(newIndex));
            newIndex++;
            oldIndex++;
        }
        while (newIndex < pl.size()) {
            //peak did not match peaks in old list
            newPeaks.addPeak(pl.get(newIndex));
            newIndex++;
        }
        //asssess quality for new list
        log.info("assessing quality for new peaks");
        List<Replicate> replicates = dm.getReplicates();
        int numberOfReplicates = replicates.size();
        for (int r_index = 0; r_index < numberOfReplicates; r_index++) {
            log.log(Level.INFO, "Replicate {0}", r_index);
            if (!replicates.get(r_index).isActive()) {
                continue;
            }
            calcMedianPeakQualityAndReplicateQuality(replicates.get(r_index).getExperiment().getDescription(), newPeaks, r_index, true, false);
            calcMedianPeakQualityAndReplicateQuality(replicates.get(r_index).getBackground().getDescription(), newPeaks, r_index, false, false);
        }
        log.info("quality assessed");

        log.info("start merging old and new peaks");
        PeakList qualityAssessed = new PeakList();
        newIndex = 0;
        oldIndex = 0;
        while (newIndex < newPeaks.size() && oldIndex < oldPeaks.size()) {
            if (chrOrder.get(newPeaks.get(newIndex).getChr()) < chrOrder.get(oldPeaks.get(oldIndex).getChr())) {
                qualityAssessed.addPeak(newPeaks.get(newIndex));
                newIndex++;
                continue;
            }
            if (chrOrder.get(oldPeaks.get(oldIndex).getChr()) < chrOrder.get(newPeaks.get(newIndex).getChr())) {
                qualityAssessed.addPeak(oldPeaks.get(oldIndex));
                oldIndex++;
                continue;
            }
            if (oldPeaks.get(oldIndex).getEnd() < newPeaks.get(newIndex).getStart()) {
                qualityAssessed.addPeak(oldPeaks.get(oldIndex));
                oldIndex++;
                continue;
            }
            qualityAssessed.addPeak(newPeaks.get(newIndex));
            newIndex++;
        }
        while (newIndex < newPeaks.size()) {
            qualityAssessed.addPeak(newPeaks.get(newIndex));
            newIndex++;
        }
        while (oldIndex < oldPeaks.size()) {
            qualityAssessed.addPeak(oldPeaks.get(oldIndex));
            oldIndex++;
        }
        peakList = qualityAssessed;
        log.info("old and new peaks merged");

        //make boxplots
        log.info("start boxplots calculation");
        PeakQuality pq = new PeakQuality();
        for (int r_index = 0; r_index < dm.getReplicates().size(); r_index++) {
            if (!dm.getReplicates().get(r_index).isActive()) {
                continue;
            }
            log.log(Level.INFO, "Replicate: {0}", r_index);
            List<Integer> qualityExperiment = calcReplicateQuality(qualityAssessed, r_index, true);
            List<Integer> qualityBackground = calcReplicateQuality(qualityAssessed, r_index, false);
            pq.addExperimentFor(SuperDuperQualityHelper.makeBoxplotData(qualityExperiment), r_index);
            pq.addBackgroundFor(SuperDuperQualityHelper.makeBoxplotData(qualityBackground), r_index);
        }
        log.info("boxplots done");

        return pq;
    }

    /**
     * Calculate quality of replicate.
     * Expects median qualities to be already calculated before!
     *
     * @param pl peak list
     * @param r_index replicate index
     * @param exp true iff experiment
     * @return qualities for replicate
     */
    private List<Integer> calcReplicateQuality(PeakList pl, int r_index, boolean exp) {
        List<Integer> medianQualities = new Vector<>();
        for (int i = 0; i <= QualityCounter.PHRED_MAX; i++) {
            medianQualities.add(0);
        }
        if (exp) {
            for (Peak p : pl.getPeaks()) {
                try {
                    int quality = p.getExperimentQuality(r_index);
                    medianQualities.set(quality, medianQualities.get(quality) + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (Peak p : pl.getPeaks()) {
                try {
                    int quality = p.getBackgroundQuality(r_index);
                    medianQualities.set(quality, medianQualities.get(quality) + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return medianQualities;
    }

    /**
     * Calculate median peak and replicate quality (one step).
     *
     * @param file filename
     * @param pl peak list
     * @param r_index replicate index
     * @param exp true iff is experiment
     * @return qualities for replicate
     */
    private List<Integer> calcMedianPeakQualityAndReplicateQuality(
            String file,
            PeakList pl,
            int r_index,
            boolean exp,
            boolean computeMedianQualities
    ) {
        final List<Integer> medianQualities = new Vector<>();
        if (computeMedianQualities) {
            for (int i = 0; i <= QualityCounter.PHRED_MAX; i++) {
                medianQualities.add(0);
            }
        }
        final int stepSize = Math.min(CHUNK_SIZE, pl.size() / dm.getNumCoresPeakQuality());
        Logger log = Logger.getLogger("calcMedianPeakQualityAndReplicateQuality");
        log.log(Level.INFO, "Step size {0}", stepSize);

        //Parallel2 parallel = new Parallel2(dm.getNumCores());
        Parallel2 p2 = ParallelizationFactory.getInstance(dm.getNumCoresPeakQuality());
        new ParallelForInt2(p2, 0, pl.size(), stepSize).loop(new IterationInt() {
            @Override
            public void iteration(int plStartIndex) {
                int plEndIndex = Math.min(plStartIndex + stepSize - 1, pl.size() - 1);
                try (SamReader samReader = SuperDuperQualityHelper.samReaderFactory.open(new File(file))) {

                    if (pl.get(plStartIndex).getChr().equals(pl.get(plEndIndex).getChr())) {
                        computeIntervalQuality(plStartIndex, plEndIndex, samReader, computeMedianQualities);
                    } else {
                        int chrStartIndex = plStartIndex;
                        int chrEndIndex = chrStartIndex;
                        while (chrEndIndex <= plEndIndex) {
                            String chr = pl.get(chrStartIndex).getChr();
                            do {
                                ++chrEndIndex;
                            } while (chrEndIndex <= plEndIndex
                                     && chr.equals(pl.get(chrEndIndex).getChr()));
                            --chrEndIndex;
                            // Compute quality
                            computeIntervalQuality(chrStartIndex, chrEndIndex, samReader, computeMedianQualities);

                            // next chromosome
                            chrStartIndex = chrEndIndex + 1;
                            chrEndIndex = chrStartIndex;
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            /**
             *
             * @param plStartIndex peak list start index
             * @param plEndIndex peak list end index
             * @param samReader sam reader
             */
            private void computeIntervalQuality(
                    int plStartIndex,
                    int plEndIndex,
                    final SamReader samReader,
                    boolean computeMedianQualities
            ) {
                List<Interval> intervalList = new ArrayList<>();
                intervalList.add(
                        new Interval(
                                pl.get(plStartIndex).getChr(),
                                pl.get(plStartIndex).getStart(),
                                pl.get(plEndIndex).getEnd()
                        ));

                Queue<SAMRecord> recordsStart;
                Queue<SAMRecord> recordsEnd;
                // prepare priority queues for counting
                try (CloseableIterator<SAMRecord> overlapping = SuperDuperQualityHelper.samRecordIntervalIteratorFactory.makeSamRecordIntervalIterator(samReader, intervalList, true)) {
                    // prepare priority queues for counting
                    recordsStart = new PriorityQueue<>(QUEUE_SIZE, compareSAMRecordByStart);
                    recordsEnd = new PriorityQueue<>(QUEUE_SIZE, compareSAMRecordByEnd);
                    // insert records sorted by start value in start queue
                    while (overlapping.hasNext()) {
                        recordsStart.add(overlapping.next());
                    }
                }

                for (int peakIndex = plStartIndex; peakIndex <= plEndIndex; ++peakIndex) {
                    Peak peak = pl.get(peakIndex);

                    // add SAMRecords starting before end of peak
                    while (!recordsStart.isEmpty() && recordsStart.peek().getStart() <= peak.getEnd()) {
                        recordsEnd.add(recordsStart.poll());
                    }

                    // remove SAMRecords ending before start of peak
                    while (!recordsEnd.isEmpty() && recordsEnd.peek().getEnd() < peak.getStart()) {
                        recordsEnd.poll();
                    }

                    int medianQuality = computeQualitiesForPeak(peak, recordsEnd);

                    if (exp) {
                        peak.addExperimentQuality(r_index, medianQuality);
                    } else {
                        peak.addBackgroundQuality(r_index, medianQuality);
                    }

                    if (computeMedianQualities) {
                        synchronized (medianQualities) {
                            medianQualities.set(medianQuality, medianQualities.get(medianQuality) + 1);
                        }
                    }
                }
            }
        });

        return medianQualities;
    }

    /**
     * Compute qualities for one peak.
     *
     * @param p peak
     * @param reads all reads
     * @return qualities for peak
     */
    private int computeQualitiesForPeak(
            Peak p,
            Iterable<SAMRecord> reads
    ) {
        QualityCounter qc = new QualityCounter();
        qc.countSAM(reads);

        int medianQuality = qc.getMedian();
        return medianQuality;
    }
}
