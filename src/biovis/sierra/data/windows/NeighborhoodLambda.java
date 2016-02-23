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
package biovis.sierra.data.windows;

import biovis.sierra.data.Replicate;
import java.util.List;

/**
 *
 * @author Dirk Zeckzer
 */
public class NeighborhoodLambda {

    private int neighborhoodRange = 0;

    private List<Window> windows = null;
    private int firstNeighbor = 0;
    private int lastNeighbor = 0;

    private List<Replicate> replicates = null;

    double tags[];

    /**
     * Constructor
     *
     * @param windows window list
     * @param neighborhoodRange size of neighborhood
     * @param replicates replicate list
     */
    public NeighborhoodLambda(
            List<Window> windows,
            int neighborhoodRange,
            List<Replicate> replicates
    ) {
        this.windows = windows;
        this.neighborhoodRange = neighborhoodRange;
        this.replicates = replicates;
        tags = new double[replicates.size() * 2];
        int controlTag;
        for (Replicate replicate : replicates) {
            controlTag = replicate.getBackground().getIndex();
            tags[controlTag] = windows.get(0).getTagCount(controlTag);
        }
    }

    /**
     * Compute lamba for window.
     *
     * @param window window to assess
     * @param windowIndex
     * @return lambda for all data sets
     */
    public double[] computeLambda(
            Window window,
            int windowIndex
    ) {
        int controlTag;

        int windowSize = window.getEnd() - window.getStart() + 1;
        // distance to neighbor window
        int distance = (neighborhoodRange - windowSize) / 2;

        /** It is expected that all windows belong to the same chromosome!
         String chr = window.getChr();
         */
        // advance first neighbor, must be less than or equal to the current window
        while (firstNeighbor < windowIndex
               && (//!chr.equals(windows.get(firstNeighbor).getChr())
                   //||
                   window.getStart() - windows.get(firstNeighbor).getEnd() > distance)) {
            for (Replicate replicate : replicates) {
                controlTag = replicate.getBackground().getIndex();
                tags[controlTag] -= windows.get(firstNeighbor).getTagCount(controlTag);
            }
            ++firstNeighbor;
        }

        while (lastNeighbor + 1 < windows.size()
               //&& chr.equals(windows.get(lastNeighbor + 1).getChr())
               && windows.get(lastNeighbor + 1).getStart() - window.getEnd() <= distance) {
            ++lastNeighbor;
            for (Replicate replicate : replicates) {
                controlTag = replicate.getBackground().getIndex();
                tags[controlTag] += windows.get(lastNeighbor).getTagCount(controlTag);
            }
        }

        double lambda[] = new double[replicates.size() * 2];
        double count;
        for (Replicate replicate : replicates) {
            controlTag = replicate.getBackground().getIndex();
            count = lastNeighbor - firstNeighbor + 1.0;
            lambda[controlTag] = tags[controlTag] / count;
        }

        return lambda;
    }
}
