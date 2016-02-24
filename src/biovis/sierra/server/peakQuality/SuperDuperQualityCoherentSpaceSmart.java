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
import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.Peak;
import biovis.sierra.data.peakcaller.PeakList;
import biovis.sierra.data.peakcaller.PeakQuality;
import biovislib.parallel4.IterationInt;
import biovislib.parallel4.Parallel2;
import biovislib.parallel4.ParallelForInt2;
import biovislib.parallel4.ParallelizationFactory;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;
import java.util.logging.Logger;

import java.util.logging.Level;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class SuperDuperQualityCoherentSpaceSmart {

    //private final static int CHUNK_SIZE = 1;
    private final static int CHUNK_SIZE = 1000;
    private final static int QUEUE_SIZE = 10000;

    // Comparator for start and end queue
    private static Comparator<SAMRecord> compareSAMRecordByEnd = (s1, s2) -> Integer.compare(s1.getEnd(), s2.getEnd());

    private DataMapper dm;
    private PeakList peakList;

    /**
     * Constructor.
     *
     * @param dm data mapper
     * @param peakList peak list
     */
    public SuperDuperQualityCoherentSpaceSmart(
            DataMapper dm,
            PeakList peakList
    ) {
        this.dm = dm;
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

        PeakQuality pq = new PeakQuality();

        int r_index = 0;
        for (Replicate r : dm.getReplicates()) {
            //log.log(Level.INFO, "starting evaluation of peak list for replicate {0}", r.getExperimentDescription());
            if (!r.isActive()) {
                r_index++;
                continue;
            }
            List<Integer> medianBackgroundQualities = calcMedianPeakQualityAndReplicateQuality(r.getBackground().getDescription(), peakList, r_index, false);
            log.log(Level.INFO, "Evaluation of peak list for replicate background {0} done", r.getBackground().getDescription());
            List<Integer> medianExperimentQualities = calcMedianPeakQualityAndReplicateQuality(r.getExperiment().getDescription(), peakList, r_index, true);
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
            boolean exp
    ) {
        final List<Integer> medianQualities = new Vector<>();
        for (int i = 0; i <= QualityCounter.PHRED_MAX; i++) {
            medianQualities.add(0);
        }
        final int stepSize = Math.min(CHUNK_SIZE, pl.size() / dm.getNumCoresPeakQuality());
        Logger log = Logger.getLogger("calcMedianPeakQualityAndReplicateQuality");
        log.log(Level.INFO, "Step size {0}", stepSize);

        final int[] max = {0, 0};

        //Parallel2 parallel = new Parallel2(1);
        Parallel2 p2 = ParallelizationFactory.getInstance(dm.getNumCoresPeakQuality());
        new ParallelForInt2(p2, 0, pl.size(), stepSize).loop(new IterationInt() {
            @Override
            public void iteration(int plStartIndex) {
                int plEndIndex = Math.min(plStartIndex + stepSize - 1, pl.size() - 1);
                try (SamReader samReader = SuperDuperQualityHelper.samReaderFactory.open(new File(file))) {

                    if (pl.get(plStartIndex).getChr().equals(pl.get(plEndIndex).getChr())) {
                        computeIntervalQuality(plStartIndex, plEndIndex, samReader);
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
                    int plStartIndex,
                    int plEndIndex,
                    final SamReader samReader            ) {
                List<Interval> intervalList = new ArrayList<>();
                for (int plIndex = plStartIndex;
                     plIndex <= plEndIndex;
                     ++plIndex) {
                    intervalList.add(
                            new Interval(
                                    pl.get(plIndex).getChr(),
                                    pl.get(plIndex).getStart(),
                                    pl.get(plIndex).getEnd()
                            ));
                }

                try (CloseableIterator<SAMRecord> overlapping = SuperDuperQualityHelper.samRecordIntervalIteratorFactory.makeSamRecordIntervalIterator(samReader, intervalList, true)) {
                    // prepare priority queues for counting
                    Queue<SAMRecord> recordsEnd = new PriorityQueue<>(QUEUE_SIZE, compareSAMRecordByEnd);

                    // last unused SAM record from iterator
                    SAMRecord lastUnused = null;

                    for (int peakIndex = plStartIndex; peakIndex <= plEndIndex; ++peakIndex) {
                        Peak peak = pl.get(peakIndex);

                        // remove SAMRecords ending before start of peak
                        while (!recordsEnd.isEmpty()
                               && recordsEnd.peek().getEnd() < peak.getStart()) {
                            recordsEnd.poll();
                        }

                        // handle last unused read from previous polls
                        if (lastUnused != null
                            && lastUnused.getStart() <= peak.getEnd()) {
                            // outside previous window
                            if (lastUnused.getEnd() >= peak.getStart()) {
                                recordsEnd.add(lastUnused);
                                lastUnused = null;
                            } else {
                                lastUnused = null;
                            }
                        }

                        // iterate over reads
                        while (lastUnused == null && overlapping.hasNext()) {
                            lastUnused = overlapping.next();
                            if (lastUnused.getStart() <= peak.getEnd()) {
                                if (lastUnused.getEnd() >= peak.getStart()) {
                                    recordsEnd.add(lastUnused);
                                    lastUnused = null;
                                } else {
                                    lastUnused = null;
                                }
                            } else {
                                break;
                            }
                        }

                        synchronized (max) {
                            if (max[1] < recordsEnd.size()) {
                                max[1] = recordsEnd.size();
                            }
                        }

                        int medianQuality = computeQualitiesForReads(recordsEnd);

                        synchronized (peak) {
                            if (exp) {
                                peak.addExperimentQuality(r_index, medianQuality);
                            } else {
                                peak.addBackgroundQuality(r_index, medianQuality);
                            }
                        }

                        synchronized (medianQualities) {
                            medianQualities.set(medianQuality, medianQualities.get(medianQuality) + 1);
                        }
                    }
                }
            }
        });

        log.log(Level.INFO, "max queues for {0}: {1} -- {2}", new Object[]{r_index, max[0], max[1]});

        return medianQualities;
    }

    /**
     * Compute qualities for one peak.
     *
     * @param reads all reads
     * @return qualities for peak
     */
    private int computeQualitiesForReads(
            Iterable<SAMRecord> reads
    ) {
        QualityCounter qc = new QualityCounter();
        qc.countSAM(reads);

        int medianQuality = qc.getMedian();
        return medianQuality;
    }
}
