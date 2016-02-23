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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Dirk Zeckzer
 */
public class NeighborhoodFactory {

    private int neighborhoodRange = 0;

    /**
     * Constructor
     *
     * @param neighborhoodRange size of neighborhood
     */
    public NeighborhoodFactory(
            int neighborhoodRange
    ) {
        this.neighborhoodRange = neighborhoodRange;
    }

    /**
     * Compute neighborhood for all windows in window list
     *
     * @param windowList window list
     * @return list of neighbors
     */
    public Map<Window, Neighborhood> computeNeighborhood(
            WindowList windowList
    ) {
        List<Window> windows = windowList.getWindows();
        Map<Window, Neighborhood> neighbors = new HashMap<>(windows.size());
        int firstNeighbor = 0;
        int lastNeighbor = 0;
        int currentWindowIndex = 0;
        List<Window> chrWindows = null;

        for (ChromosomeWindowRange windowRange : windowList.getChromosomeWindows()) {
            chrWindows = windowList.getWindows(windowRange.getFirst(), windowRange.getLast());
            currentWindowIndex = windowRange.getFirst();
            firstNeighbor = currentWindowIndex;
            lastNeighbor = currentWindowIndex;
            for (Window currentWindow : chrWindows) {
                int windowSize = currentWindow.getEnd() - currentWindow.getStart() + 1;
                // distance to neighbor window
                int distance = (neighborhoodRange - windowSize) / 2;

                // advance first neighbor, must be less than or equal to the current window
                while (firstNeighbor < currentWindowIndex
                       && currentWindow.getStart() - windows.get(firstNeighbor).getEnd() > distance) {
                    ++firstNeighbor;
                }

                // advance last neighbor, must be equal to or greater than the current window
                if (lastNeighbor < currentWindowIndex) {
                    lastNeighbor = currentWindowIndex;
                }
                while (lastNeighbor + 1 < windowRange.getLast()
                       && windows.get(lastNeighbor + 1).getStart() - currentWindow.getEnd() <= distance) {
                    ++lastNeighbor;
                }

                // Add neighborhood
                neighbors.put(currentWindow, new Neighborhood(firstNeighbor, lastNeighbor));

                ++currentWindowIndex;
            }
        }
        return neighbors;
    }
}
