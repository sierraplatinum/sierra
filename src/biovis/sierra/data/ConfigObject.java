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
package biovis.sierra.data;

import java.util.ArrayList;

/**
 *
 * @author Daniel Gerighausen
 */
public class ConfigObject {
	ArrayList<SmallReplicate> replicates;
	private int windowsize;
	private int offset;
	private double pvaluecutoff;
	private boolean peakmode;
	private int numCores;
	private String jobName;
        /**
         * Constructor.
         */
	public ConfigObject()
	{
		replicates = new ArrayList<>();
	}

	public void addReplicate(String experiment, String background, String name)
	{
		replicates.add(new SmallReplicate(experiment, background, name));
	}

	public void setSettings(int windowsize, int offset, double pvaluecutoff, int numCores, String jobName)
	{
		this.windowsize = windowsize;
		this.offset = offset;
		this.pvaluecutoff = pvaluecutoff;
		this.numCores = numCores;
		this.jobName = jobName;
		//System.err.println(this.offset);
		//System.err.println(this.windowsize);
	}

	public int getWindowsize() {
		//System.err.println("get windowsize config");
		//System.err.println(this.windowsize);
		return windowsize;
	}

	public int getOffset() {
		//System.err.println("get offset config");
		//System.err.println(this.offset);
		return offset;
	}

	public boolean getpeakmode() {
		return peakmode;
	}

	public double getPvaluecutoff() {
		return pvaluecutoff;
	}

	public int getNumCores() {
		//System.err.println(numCores);
		return numCores;
	}

	public void setNumCores(int numCores) {
		this.numCores = numCores;
	}

        public ArrayList<SmallReplicate> getReplicates()
	{
		return replicates;
	}


	public class SmallReplicate {
		private String experiment, background, name;

		public SmallReplicate(String experiment, String background, String name)
		{
			this.experiment= experiment;
			this.background = background;
			this.name = name;
		}

		public String getExperiment()
		{
			return experiment;
		}

		public String getBackground()
		{
			return background;
		}

		public String getName() {
			return name;
		}

	
	}


	public String getJobName() {
		return jobName;
	}
}
