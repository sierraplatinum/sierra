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
package biovis.sierra.server.peakMe;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.PoissonDistributionCollection;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.data.windows.Neighborhood;
import biovis.sierra.data.windows.NeighborhoodFactory;
import java.util.Map;
import parallel4.IterationInt;
import parallel4.Parallel2;
import parallel4.ParallelForInt2;
import parallel4.ParallelizationFactory;

/**
 *
 * @author Lydia Mueller
 */
public class PeakMe {

    private WindowList windowList;
    private PoissonDistributionCollection collection;
    private DataMapper mapper;

    Map<Window, Neighborhood> neighbors1k;
    Map<Window, Neighborhood> neighbors5k;
    Map<Window, Neighborhood> neighbors10k;

    /**
     * Constructor.
     *
     * @param wl window list
     * @param pdc poisson distribution
     * @param dm data mapper
     */
    public PeakMe(
            WindowList wl,
            PoissonDistributionCollection pdc,
            DataMapper dm
    ) {
        windowList = wl;
        collection = pdc;
        mapper = dm;
        NeighborhoodFactory neighborhood;

        // 1k neighborhood
        neighborhood = new NeighborhoodFactory(1000);
        neighbors1k = neighborhood.computeNeighborhood(windowList);
        // 5k neighborhood
        neighborhood = new NeighborhoodFactory(5000);
        neighbors5k = neighborhood.computeNeighborhood(windowList);
        // 10k neighborhood
        neighborhood = new NeighborhoodFactory(10000);
        neighbors10k = neighborhood.computeNeighborhood(windowList);
    }

    /**
     * Compute p-values for windows.
     */
    public void peak() {
        for (Replicate replicate : mapper.getReplicates()) {
            final int expTag = replicate.getExperiment().getIndex();
            final int controlTag = replicate.getBackground().getIndex();
            int stepSize = windowList.getSize() / mapper.getNumCores() + 1;
            Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
            new ParallelForInt2(p2, 0, windowList.getSize(), stepSize).loop(
                    new IterationInt() {
                        @Override
                        public void iteration(int index) {
                            int end = Math.min(index + stepSize, windowList.getSize());
                            for (Window window : windowList.getWindows(index, end)) {
                                double lambda = getMaxLambda(controlTag, window);
                                double count = window.getTagCount(expTag);
                                double pval = collection.getPValue(controlTag, lambda, count);
                                window.setRawPValue(replicate.getIndex(), pval);
                            }
                        }
                    });
        }
    }

    /**
     * Compute maximum lambda.
     *
     * @param controlTag tag
     * @param win window
     * @return maximum lambda
     */
    private double getMaxLambda(int controlTag, Window win) {
        double lambda1k = 0;
        double lambda5k = 0;
        double lambda10k = 0;

        //number of windows in neighborhoods of size 1k, 5k,10k
        double count1k = neighbors1k.get(win).getLast() - neighbors1k.get(win).getFirst() + 1.0;
        double count5k = neighbors5k.get(win).getLast() - neighbors5k.get(win).getFirst() + 1.0;
        double count10k = neighbors10k.get(win).getLast() - neighbors10k.get(win).getFirst() + 1.0;

        //sum up tag counts in neighborhoods
        for (Window w : windowList.getWindows(neighbors1k.get(win).getFirst(), neighbors1k.get(win).getLast() + 1)) {
            lambda1k += w.getTagCount(controlTag);
        }
        for (Window w : windowList.getWindows(neighbors5k.get(win).getFirst(), neighbors5k.get(win).getLast() + 1)) {
            lambda5k += w.getTagCount(controlTag);
        }
        for (Window w : windowList.getWindows(neighbors10k.get(win).getFirst(), neighbors10k.get(win).getLast() + 1)) {
            lambda10k += w.getTagCount(controlTag);
        }

        //calculate average tag count
        lambda1k /= count1k;
        lambda5k /= count5k;
        lambda10k /= count10k;

        return Math.max(Math.max(collection.getLambdaByTag(controlTag), lambda1k), Math.max(lambda5k, lambda10k));
    }
}
