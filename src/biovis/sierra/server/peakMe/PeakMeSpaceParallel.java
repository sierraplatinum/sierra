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
import biovis.sierra.data.windows.ChromosomeWindowRange;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.data.windows.NeighborhoodLambda;
import biovislib.parallel4.IterationParameter;
import biovislib.parallel4.Parallel2;
import biovislib.parallel4.ParallelForParameter;
import biovislib.parallel4.ParallelizationFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lydia Mueller
 */
public class PeakMeSpaceParallel {

    private WindowList windowList;
    private DataMapper mapper;

    /**
     * Constructor.
     *
     * @param wl window list
     * @param dm data mapper
     */
    public PeakMeSpaceParallel(
            WindowList wl,
            DataMapper dm
    ) {
        windowList = wl;
        mapper = dm;
    }

    /**
     * Compute p-values for windows.
     */
    public void peak() {
        final List<ChromosomeWindowRange> chromosomeWindows = new ArrayList<>(windowList.getChromosomeWindows());
        Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
        new ParallelForParameter<>(p2, chromosomeWindows).loop(
                new IterationParameter<ChromosomeWindowRange>() {
            @Override
            public void iteration(ChromosomeWindowRange windowRange) {
                List<Window> windows = windowList.getWindows(windowRange.getFirst(), windowRange.getLast());
                NeighborhoodLambda neighbors1kFactory = new NeighborhoodLambda(windows, 1000, mapper.getReplicates());
                NeighborhoodLambda neighbors5kFactory = new NeighborhoodLambda(windows, 5000, mapper.getReplicates());
                NeighborhoodLambda neighbors10kFactory = new NeighborhoodLambda(windows, 10000, mapper.getReplicates());

                int index = windowRange.getFirst();
                double lambda1k[];
                double lambda5k[];
                double lambda10k[];
                int expTag;
                int controlTag;

                double lambda;
                double count;
                double pval;
                double countBack;
                double pvalBack;
                for (Window w : windows) {
                    // Neighborhood lambda values
                    lambda1k = neighbors1kFactory.computeLambda(w, index);
                    lambda5k = neighbors5kFactory.computeLambda(w, index);
                    lambda10k = neighbors10kFactory.computeLambda(w, index);

                    // Compute p value for all replicates
                    for (Replicate replicate : mapper.getReplicates()) {
                        expTag = replicate.getExperiment().getIndex();
                        controlTag = replicate.getBackground().getIndex();

                        lambda = Math.max(Math.max(replicate.getBackground().getLambdaFromPoisson(), lambda1k[controlTag]),
                                          Math.max(lambda5k[controlTag], lambda10k[controlTag]));
                        countBack = w.getTagCount(controlTag);
                        pvalBack = replicate.getBackground().getPValue(lambda, countBack);

                        if (pvalBack < mapper.getPvaluecutoff()) {
                        	lambda = countBack;
                        }
                        count = w.getTagCount(expTag);
                        pval = replicate.getBackground().getPValue(lambda, count);
                        w.setRawPValue(replicate.getIndex(), pval);
                    }
                    ++index;
                }
            }
        });
    }
}
