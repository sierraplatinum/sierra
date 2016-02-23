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

/**
 *
 * @author Lydia Mueller
 */
public class IntBoxplotData {
	private int min;
	private int max;
	private int median;
	private int upperQuartile;
	private int lowerQuartile;

        /**
         * Default constructor.
         */
	public IntBoxplotData() {
            this(-1, -1, -1, -1, -1);
	}

	/**
         * Constructor
         *
         * @param min minimum
         * @param max maximum
         * @param median median
         * @param upperQuartile upper quartile
         * @param lowerQuartile lower quartile
         */
	public IntBoxplotData(
                int min,
                int max,
                int median,
                int upperQuartile,
		int lowerQuartile
        ) {
		this.min = min;
		this.max = max;
		this.median = median;
		this.upperQuartile = upperQuartile;
		this.lowerQuartile = lowerQuartile;
	}

        public int getMin() {
		return min;
	}

        public void setMin(int min) {
		this.min = min;
	}

        public int getMax() {
		return max;
	}

        public void setMax(int max) {
		this.max = max;
	}

        public int getMedian() {
		return median;
	}

        public void setMedian(int median) {
		this.median = median;
	}

        public int getUpperQuartile() {
		return upperQuartile;
	}

        public void setUpperQuartile(int upperQuartile) {
		this.upperQuartile = upperQuartile;
	}

        public int getLowerQuartile() {
		return lowerQuartile;
	}

        public void setLowerQuartile(int lowerQuartile) {
		this.lowerQuartile = lowerQuartile;
	}

	@Override
	public String toString() {
		return "IntBoxplotData [min=" + min
                        + ", max=" + max
                        + ", median=" + median
                        + ", upperQuartile=" + upperQuartile
			+ ", lowerQuartile=" + lowerQuartile
                        + "]";
	}
}
