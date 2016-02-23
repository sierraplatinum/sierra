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

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class OverlapRatio {

    /**
     * Estimate overlap ratio for each replicate.
     * @param mapper data mapper
     * @param wl window list
     */
    public static void estimateOverlapRatio(
            DataMapper mapper,
            WindowList wl
    ) {
        long sigs = 0;
        List<Long> overlap = new ArrayList<>();
        List<Replicate> replicates = mapper.getReplicates();
        int numberOfReplicates = replicates.size();
        for (int i = 0; i < numberOfReplicates; i++) {
            overlap.add((long) 0);
        }

        double cutoffPValue = mapper.getPvaluecutoff();
        for (Window w : wl.getWindows()) {
            if (w.getFinalPValue() < cutoffPValue) {
                sigs++;
                for (int i = 0; i < numberOfReplicates; i++) {
                    if (w.getPValueList()[i] < cutoffPValue) {
                        overlap.set(i, overlap.get(i) + 1);
                    }
                }
            }
        }

        HashMap<Integer, Double> overlapRatio = new HashMap<>();
        for (int i = 0; i < numberOfReplicates; i++) {
            if (replicates.get(i).isActive()) {
            	if(sigs > 0){
            		overlapRatio.put(i, (double) overlap.get(i) / sigs);
            	}
            } else {
                overlapRatio.put(i, 0.0);
            }
        }

        mapper.setOverlapWithReplicates(overlapRatio);
    }

}
