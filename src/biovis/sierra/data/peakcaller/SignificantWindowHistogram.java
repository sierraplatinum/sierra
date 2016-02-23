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
import java.util.List;

/**
 *
 * @author Lydia Mueller
 */
public class SignificantWindowHistogram  {
	private List<Integer> sigWindowsX;
	private List<Integer> sigWindowsY;
	private int median;

        /**
         * Constructor.
         *
         * @param sigWindowsX significant windows x
         * @param sigWindowsY significant windows y
         * @param median median
         */
	public SignificantWindowHistogram(
            List<Integer> sigWindowsX,
            List<Integer> sigWindowsY,
            int median
        ) {
		this.sigWindowsX = sigWindowsX;
		this.sigWindowsY = sigWindowsY;
		this.median = median;
	}

	public List<Integer> getSigWindowsX() {
		return sigWindowsX;
	}

	public void setSigWindowsX(List<Integer> sigWindowsX) {
		this.sigWindowsX = sigWindowsX;
	}

	public List<Integer> getSigWindowsY() {
		return sigWindowsY;
	}

	public void setSigWindowsY(List<Integer> sigWindowsY) {
		this.sigWindowsY = sigWindowsY;
	}

	public int getMedian() {
		return median;
	}

	public void setMedian(int median) {
		this.median = median;
	}
}
