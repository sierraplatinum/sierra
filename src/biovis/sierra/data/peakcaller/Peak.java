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

import java.util.Vector;

import biovis.sierra.data.windows.Window;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lydia Mueller
 */
public class Peak {
	private String chr;
	private int start;
	private int end;
	private double pvalue;
	private List<Integer> medianBackgroundQuality;
	private List<Integer> medianExperimentQuality;

        /**
         * Constructor.
         *
	 * @param chr chromosome
	 * @param start start position
	 * @param end end position
	 * @param pvalue p-value
	 */
	public Peak(String chr, int start, int end, double pvalue) {
		this.chr = chr;
		this.start = start;
		this.end = end;
		this.pvalue = pvalue;
	}

        /**
         * Constructor.
         *
         * @param w window
         */
	public Peak(Window w) {
          this.chr = w.getChr();
          this.start = w.getStart();
          this.end = w.getEnd();
          this.pvalue = w.getFinalPValue();
          medianExperimentQuality = new ArrayList<>();
          medianBackgroundQuality = new ArrayList<>();
	}

        public Peak(Peak p) {
            this.chr = p.getChr();
            this.start = p.getStart();
            this.end = p.getEnd();
            this.pvalue = p.getPvalue();
            if(p.medianExperimentQuality != null){
                medianExperimentQuality = new ArrayList<>();
                medianExperimentQuality.addAll(p.medianExperimentQuality);
            }
            if(p.medianBackgroundQuality != null){
                medianBackgroundQuality = new ArrayList<>();
                medianBackgroundQuality.addAll(p.medianBackgroundQuality);
            }
        }

        @Override
	public String toString() {
		return "Peak [chr = " + chr + " start=" + start + ", end=" + end + "]";
	}

        /**
         * Is another peak mergeable to this one.
         *
         * @param p second peak
         * @return true iff mergeable
         */
	public boolean mergeable(Peak p){
		if(!p.getChr().equals(chr))return false;
		if(p.getEnd() < start)return false;
		if(p.getStart() > end)return false;
		return true;
	}

        /**
         * Merge another peak to this peak.
         *
         * @param p second peak
         */
	public void merge(Peak p){
		start = Math.min(start, p.getStart());
		end = Math.max(end, p.getEnd());
		pvalue = Math.min(pvalue, p.getPvalue());
	}

	public int getEnd() {
		return end;
	}

	public String getChr() {
		return chr;
	}

        public int getStart() {
		return start;
	}

        public double getPvalue() {
		return pvalue;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chr == null) ? 0 : chr.hashCode());
		result = prime * result + end;
		result = prime * result + start;
		return result;
	}

	public void addBackgroundQuality(int i, int quality){
            synchronized(medianBackgroundQuality) {
		while(medianBackgroundQuality.size() <= i){
			medianBackgroundQuality.add(-1);
		}
		medianBackgroundQuality.set(i, quality);
            }
	}

	public void addExperimentQuality(int i, int quality){
            synchronized(medianExperimentQuality) {
		while(medianExperimentQuality.size() <= i){
			medianExperimentQuality.add(-1);
		}
		medianExperimentQuality.set(i, quality);
            }
	}

	public int  getBackgroundQuality(int i){
            synchronized(medianBackgroundQuality) {
		return medianBackgroundQuality.get(i);
            }
	}

	public int getExperimentQuality(int i){
            synchronized(medianExperimentQuality) {
		return medianExperimentQuality.get(i);
            }
	}

        /* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Peak other = (Peak) obj;
		if (chr == null) {
			if (other.chr != null)
				return false;
		} else if (!chr.equals(other.chr))
			return false;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}
}
