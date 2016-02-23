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

import biovis.sierra.data.peakcaller.Peak;
import biovis.sierra.data.peakcaller.PeakList;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class PeakFactory {

    private final static int DISTANCE_FACTOR = 2;

    private PeakList peakListNarrow = new PeakList();
    private PeakList peakListBroad = new PeakList();

    // Threshold for peaks
    private double threshold = 1e-5;

    // Window list
    private WindowList windowList;

    // max distance between two windows of a broad peak
    private int maxDistance;

    /**
     * Constructor.
     *
     * @param wl window list
     * @param windowSize size of a window
     */
    public PeakFactory(
            WindowList wl,
            int windowSize
    ) {
        windowList = wl;
        maxDistance = windowSize;
        maxDistance *= DISTANCE_FACTOR;
    }

    /**
     * Set new threshold.
     *
     * @param t new threshold
     */
    public void setThreshold(double t) {
        threshold = t;
        peakListBroad.clear();
        peakListNarrow.clear();
    }

    /**
     * get narrow peak list.
     *
     * @return narrow peak list
     */
    public PeakList getPeakListNarrow() {
        if (peakListNarrow.isEmpty()) {
            generatePeakListNarrow();
        }
        return peakListNarrow;
    }

    /**
     * Generate list with narrow peaks.
     */
    private void generatePeakListNarrow() {
        Peak last = null;
        for (Window window : windowList.getWindows()) {
            if (window.getFinalPValue() <= threshold) {
                Peak current = new Peak(window);
                if (last != null && last.mergeable(current)) {
                    last.merge(current);
                } else {
                    if (last != null) {
                        peakListNarrow.addPeak(last);
                    }
                    last = current;
                }
            }
        }
        if (last != null) {
            peakListNarrow.addPeak(last);
        }
    }

    /**
     * get broad peak list.
     *
     * @return broad peak list
     */
    public PeakList getPeakListBroad() {
        if (peakListBroad.isEmpty()) {
            generatePeakListBroad();
        }
        return peakListBroad;
    }

    /**
     * Generate list with broad peaks.
     */
    private void generatePeakListBroad() {
        if (peakListNarrow.isEmpty()) {
            generatePeakListNarrow();
        }
        if (peakListNarrow.isEmpty()) {
            return;
        }
        Peak last = new Peak(peakListNarrow.get(0));
        for (int i = 1; i < peakListNarrow.size(); i++) {
            Peak current = peakListNarrow.get(i);
            if (last.getChr().equals(current.getChr())
                && (current.getStart() - last.getEnd()) < maxDistance) {
                last.merge(current);
            } else {
                peakListBroad.addPeak(last);
                last = new Peak(current);
            }
        }
        if (last != null) {
            peakListBroad.addPeak(last);
        }
    }
}
