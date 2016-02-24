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
package biovis.sierra.data.peakcaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Lydia Mueller
 */
public class PeakQuality {
	private List<IntBoxplotData> background;
	private List<IntBoxplotData> experiment;
	private Map<Integer,Integer> replicates;

        /**
         * Constructor.
         */
	public PeakQuality() {
		replicates = new HashMap<>();
		background = new ArrayList<>();
		experiment = new ArrayList<>();
	}

	public Set<Integer> getReplicatesWithQuality(){
		return replicates.keySet();
	}

	public void addBackgroundFor(IntBoxplotData bp, int rep){
		if(replicates.containsKey(rep)){
			background.set(replicates.get(rep), bp);
		}else{
			replicates.put(rep, background.size());
			background.add(bp);
			experiment.add(new IntBoxplotData());
		}
	}

	public void addExperimentFor(IntBoxplotData bp, int rep){

		if(replicates.containsKey(rep)){
			experiment.set(replicates.get(rep), bp);
		}else{
			replicates.put(rep, experiment.size());
			experiment.add(bp);
			background.add(new IntBoxplotData());
		}
	}

	public IntBoxplotData getBackgroundFor(int rep){
		if(replicates.containsKey(rep)){
			return background.get(replicates.get(rep));
		}
		return new IntBoxplotData();
	}

	public IntBoxplotData getExperimentFor(int rep){
		if(replicates.containsKey(rep)){
			return experiment.get(replicates.get(rep));
		}
		return new IntBoxplotData();
	}
}
