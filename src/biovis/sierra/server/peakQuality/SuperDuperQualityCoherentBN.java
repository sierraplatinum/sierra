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
import java.util.Vector;
import java.util.logging.Logger;

import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.Peak;
import biovis.sierra.data.peakcaller.PeakList;
import biovis.sierra.data.peakcaller.PeakQuality;
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
public class SuperDuperQualityCoherentBN {

    //private final static int CHUNK_SIZE = 1;
    private final static int CHUNK_SIZE = 1000;
    private final static int QUEUE_SIZE = 10000;

    // Comparator for start and end queue
    private static Comparator<SAMRecord> compareSAMRecordByStart = (s1, s2) -> Integer.compare(s1.getStart(), s2.getStart());
    private static Comparator<SAMRecord> compareSAMRecordByEnd = (s1, s2) -> Integer.compare(s1.getEnd(), s2.getEnd());

    private DataMapper dm;
    private WindowList wl;
    private PeakList peakListBroad;
    private PeakList peakListNarrow;

    /**
     * Constructor.
     *
     * @param dm data mapper
     * @param wl window list
     * @param peakListBroad broad list with peaks
     * @param peakListNarrow narrow list with peaks
     */
    public SuperDuperQualityCoherentBN(
            DataMapper dm,
            WindowList wl,
            PeakList peakListBroad,
            PeakList peakListNarrow
    ) {
        this.dm = dm;
        this.wl = wl;
        this.peakListBroad = peakListBroad;
        this.peakListNarrow = peakListNarrow;
    }

    /**
     * Evaluate peak list.
     *
     * @param pqBroad
     * @param pqNarrow
     */
    public void evaluatePeakList(
            PeakQuality pqBroad,
            PeakQuality pqNarrow
    ) {
        Logger log = Logger.getLogger("starting evaluation of peak list");
        log.info("starting evaluation of peak list");

        //log.info("starting evaluation of peak list II");
        int r_index = 0;
        for (Replicate r : dm.getReplicates()) {
            //log.log(Level.INFO, "starting evaluation of peak list for replicate {0}", r.getExperimentDescription());
            if (!r.isActive()) {
                r_index++;
                continue;
            }
            List<Integer> medianQualitiesBroad = new Vector<>();
            List<Integer> medianQualitiesNarrow = new Vector<>();
            calcMedianPeakQualityAndReplicateQuality(
                    r.getBackground().getDescription(),
                    peakListBroad, peakListNarrow,
                    medianQualitiesBroad, medianQualitiesNarrow,
                    r_index,
                    false);
            pqBroad.addBackgroundFor(SuperDuperQualityHelper.makeBoxplotData(medianQualitiesBroad), r_index);
            pqNarrow.addBackgroundFor(SuperDuperQualityHelper.makeBoxplotData(medianQualitiesNarrow), r_index);
            log.log(Level.INFO, "Evaluation of peak list for replicate background {0} done", r.getBackground().getDescription());

            medianQualitiesBroad = new Vector<>();
            medianQualitiesNarrow = new Vector<>();
            calcMedianPeakQualityAndReplicateQuality(
                    r.getExperiment().getDescription(),
                    peakListBroad, peakListNarrow,
                    medianQualitiesBroad, medianQualitiesNarrow,
                    r_index,
                    true);
            pqBroad.addExperimentFor(SuperDuperQualityHelper.makeBoxplotData(medianQualitiesBroad), r_index);
            pqNarrow.addExperimentFor(SuperDuperQualityHelper.makeBoxplotData(medianQualitiesNarrow), r_index);
            log.log(Level.INFO, "Evaluation of peak list for replicate experiment {0} done", r.getExperiment().getDescription());

            log.log(Level.INFO, "Evaluation of peak list for replicate {0} done", r.getExperiment().getDescription());

            r_index++;
        }

        log.info("evaluation of peak list done");
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
    private void calcMedianPeakQualityAndReplicateQuality(
            String file,
            PeakList plBroad,
            PeakList plNarrow,
            List<Integer> medianQualitiesBroad,
            List<Integer> medianQualitiesNarrow,
            int r_index,
            boolean exp
    ) {
        if (medianQualitiesBroad != null) {
            for (int i = 0; i <= QualityCounter.PHRED_MAX; i++) {
                medianQualitiesBroad.add(0);
            }
        }
        if (medianQualitiesNarrow != null) {
            for (int i = 0; i <= QualityCounter.PHRED_MAX; i++) {
                medianQualitiesNarrow.add(0);
            }
        }

        final int stepSize = Math.min(CHUNK_SIZE, plBroad.size() / dm.getNumCoresPeakQuality());
        Logger log = Logger.getLogger("calcMedianPeakQualityAndReplicateQuality");
        log.log(Level.INFO, "Step size {0}", stepSize);

        final int[] max = {0, 0};

        //Parallel2 parallel = new Parallel2(1);
        Parallel2 p2 = ParallelizationFactory.getInstance(dm.getNumCoresPeakQuality());
        new ParallelForInt2(p2, 0, plBroad.size(), stepSize).loop(new IterationInt() {
            @Override
            public void iteration(int plStartIndex) {
                int plEndIndex = Math.min(plStartIndex + stepSize - 1, plBroad.size() - 1);
                try (SamReader samReader = SuperDuperQualityHelper.samReaderFactory.open(new File(file))) {

                    if (plBroad.get(plStartIndex).getChr().equals(plBroad.get(plEndIndex).getChr())) {
                        computeIntervalQuality(plStartIndex, plEndIndex, samReader);
                    } else {
                        int chrStartIndex = plStartIndex;
                        int chrEndIndex = chrStartIndex;
                        while (chrEndIndex <= plEndIndex) {
                            String chr = plBroad.get(chrStartIndex).getChr();
                            do {
                                ++chrEndIndex;
                            } while (chrEndIndex <= plEndIndex
                                     && chr.equals(plBroad.get(chrEndIndex).getChr()));
                            --chrEndIndex;
                            // Compute quality
                            computeIntervalQuality(chrStartIndex, chrEndIndex, samReader);

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
                    int plBroadStartIndex,
                    int plBroadEndIndex,
                    final SamReader samReader
            ) {
                // broad peaks
                ArrayList<Interval> intervalList = new ArrayList<>();
                intervalList.add(
                        new Interval(
                                plBroad.get(plBroadStartIndex).getChr(),
                                plBroad.get(plBroadStartIndex).getStart(),
                                plBroad.get(plBroadEndIndex).getEnd()
                        ));

                // prepare priority queues for counting
                Queue<SAMRecord> recordsStartBroad = new PriorityQueue<>(QUEUE_SIZE, compareSAMRecordByStart);
                Queue<SAMRecord> recordsEndBroad = new PriorityQueue<>(QUEUE_SIZE, compareSAMRecordByEnd);
                Queue<SAMRecord> recordsStartNarrow = new PriorityQueue<>(QUEUE_SIZE, compareSAMRecordByStart);
                Queue<SAMRecord> recordsEndNarrow = new PriorityQueue<>(QUEUE_SIZE, compareSAMRecordByEnd);

                // prepare priority queues for counting
                try (CloseableIterator<SAMRecord> overlapping = SuperDuperQualityHelper.samRecordIntervalIteratorFactory.makeSamRecordIntervalIterator(samReader, intervalList, true)) {
                    // insert records sorted by start value in start queue
                    while (overlapping.hasNext()) {
                        SAMRecord samRecord = overlapping.next();
                        recordsStartBroad.add(samRecord);
                        recordsStartNarrow.add(samRecord);
                    }
                }

                synchronized (max) {
                    if (max[0] < recordsStartBroad.size()) {
                        max[0] = recordsStartBroad.size();
                    }
                }

                computeQualitiesForChunk(
                        plBroad,
                        plBroadStartIndex,
                        plBroadEndIndex,
                        recordsStartBroad,
                        recordsEndBroad,
                        medianQualitiesBroad);

                // narrow peaks
                int plNarrowStartIndex = 0;
                int plNarrowEndIndex = 0;
                Peak plBroadStart = plBroad.get(plBroadStartIndex);
                Peak plBroadEnd = plBroad.get(plBroadEndIndex);
                Peak peak = null;
                for (int peakIndex = 0; peakIndex < plNarrow.size(); ++peakIndex) {
                    peak = plNarrow.get(peakIndex);
                    if (plBroadStart.getChr().equals(peak.getChr())
                            && plBroadStart.getStart() <= peak.getStart()) {
                        plNarrowStartIndex = peakIndex;
                        break;
                    }
                }
                for (int peakIndex = plNarrow.size() - 1; peakIndex >= plNarrowStartIndex; --peakIndex) {
                    peak = plNarrow.get(peakIndex);
                    if (plBroadEnd.getChr().equals(peak.getChr())
                            && peak.getEnd() <= plBroadEnd.getEnd()) {
                        plNarrowEndIndex = peakIndex;
                        break;
                    }
                }
                /*
                 System.err.print("Broad index range: " + plBroadStartIndex + "--" + plBroadEndIndex);
                 System.err.print(" | ");
                 System.err.println("Narrow index range: " + plNarrowStartIndex + "--" + plNarrowEndIndex);
                 System.err.print("Broad range: " + plBroad.get(plBroadStartIndex).getChr()
                 + "-> " + plBroad.get(plBroadStartIndex).getStart()
                 + "--" + plBroad.get(plBroadEndIndex).getEnd());
                 System.err.print(" | ");
                 System.err.println("Narrow range: " + plNarrow.get(plNarrowStartIndex).getChr()
                 + "-> " + plNarrow.get(plNarrowStartIndex).getStart()
                 + "--" + plNarrow.get(plNarrowStartIndex).getEnd()
                 + " ---- " + plNarrow.get(plNarrowEndIndex).getChr()
                 + "-> " + plNarrow.get(plNarrowEndIndex).getStart()
                 + "--" + plNarrow.get(plNarrowEndIndex).getEnd());
                 */

                computeQualitiesForChunk(
                        plNarrow,
                        plNarrowStartIndex,
                        plNarrowEndIndex,
                        recordsStartNarrow,
                        recordsEndNarrow,
                        medianQualitiesNarrow);
            }

            /**
             * Compute qualities of all peaks in one chunk.
             *
             * @param pl peak list
             * @param plStartIndex first chunk
             * @param plEndIndex last chunk
             * @param recordsStart start queue
             * @param recordsEnd end queue
             * @param medianQualities median qualities
             */
            private void computeQualitiesForChunk(
                    PeakList pl,
                    int plStartIndex,
                    int plEndIndex,
                    Queue<SAMRecord> recordsStart,
                    Queue<SAMRecord> recordsEnd,
                    List<Integer> medianQualities
            ) {
                for (int peakIndex = plStartIndex; peakIndex <= plEndIndex; ++peakIndex) {
                    Peak peak = pl.get(peakIndex);

                    // add SAMRecords starting before end of peak
                    while (!recordsStart.isEmpty()
                            && recordsStart.peek().getStart() <= peak.getEnd()) {
                        recordsEnd.add(recordsStart.poll());
                    }

                    // remove SAMRecords ending before start of peak
                    while (!recordsEnd.isEmpty()
                            && recordsEnd.peek().getEnd() < peak.getStart()) {
                        recordsEnd.poll();
                    }

                    synchronized (max) {
                        if (max[1] < recordsEnd.size()) {
                            max[1] = recordsEnd.size();
                        }
                    }

                    int medianQuality = computeQualitiesForPeak(peak, recordsEnd);

                    if (exp) {
                        peak.addExperimentQuality(r_index, medianQuality);
                    } else {
                        peak.addBackgroundQuality(r_index, medianQuality);
                    }

                    if (medianQualities != null) {
                        synchronized (medianQualities) {
                            medianQualities.set(medianQuality, medianQualities.get(medianQuality) + 1);
                        }
                    }
                }
            }
        });

        log.log(Level.INFO, "max queues for {0}: {1} -- {2}", new Object[]{r_index, max[0], max[1]});
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