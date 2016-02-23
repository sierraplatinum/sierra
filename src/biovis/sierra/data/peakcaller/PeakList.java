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

/**
 *
 * @author Lydia Mueller
 */
public class PeakList {
	private ArrayList<Peak> peaks;

	public PeakList(){
		peaks = new ArrayList<>();
	}

	public PeakList(PeakList pl){
		peaks = new ArrayList<>();
		peaks.addAll(pl.peaks);
	}
	public void addPeak(Peak p){
		peaks.add(p);
	}

	public Iterable<Peak> getPeaks() {
		return peaks;
	}

	public int size() {
		return peaks.size();
	}

	public boolean isEmpty() {
		return peaks.isEmpty();
	}

	public boolean contains(Peak peak) {
		return peaks.contains(peak);
	}

	public Peak remove(int index) {
		return peaks.remove(index);
	}

	@Override
	public String toString() {
		return "PeakList [peaks=" + peaks + "]";
	}

	public boolean remove(Peak peak) {
		return peaks.remove(peak);
	}

	public void clear() {
		peaks.clear();
	}

	public Peak get(int index) {
		return peaks.get(index);
	}
}
