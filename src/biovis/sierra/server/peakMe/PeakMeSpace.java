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
import biovis.sierra.data.windows.NeighborhoodLambda;

/**
 *
 * @author Lydia Mueller
 */
public class PeakMeSpace {

    private WindowList windowList;
    private PoissonDistributionCollection collection;
    private DataMapper mapper;

    NeighborhoodLambda neighbors1kFactory;
    NeighborhoodLambda neighbors5kFactory;
    NeighborhoodLambda neighbors10kFactory;

    /**
     * Constructor.
     *
     * @param wl window list
     * @param pdc poisson distribution
     * @param dm data mapper
     */
    public PeakMeSpace(
            WindowList wl,
            PoissonDistributionCollection pdc,
            DataMapper dm
    ) {
        windowList = wl;
        collection = pdc;
        mapper = dm;

        neighbors1kFactory = new NeighborhoodLambda(wl.getWindows(), 1000, dm.getReplicates());
        neighbors5kFactory = new NeighborhoodLambda(wl.getWindows(), 5000, dm.getReplicates());
        neighbors10kFactory = new NeighborhoodLambda(wl.getWindows(), 10000, dm.getReplicates());
    }

    /**
     * Compute p values for windows.
     */
    public void peak() {
        int index = 0;
        int end = windowList.getSize();
        for (Window w : windowList.getWindows(index, end)) {
            // Neighborhood lambda values
            double lambda1k[] = neighbors1kFactory.computeLambda(w, index);
            double lambda5k[] = neighbors5kFactory.computeLambda(w, index);
            double lambda10k[] = neighbors10kFactory.computeLambda(w, index);

            // Compute p value for all replicates
            for (Replicate replicate : mapper.getReplicates()) {
                int expTag = replicate.getExperiment().getIndex();
                int controlTag = replicate.getBackground().getIndex();

                double lambda = Math.max(Math.max(collection.getLambdaByTag(controlTag), lambda1k[controlTag]),
                                         Math.max(lambda5k[controlTag], lambda10k[controlTag]));
                double count = w.getTagCount(expTag);
                double pval = collection.getPValue(controlTag, lambda, count);
                w.setRawPValue(replicate.getIndex(), pval);
            }
            ++index;
        }
    }
}
