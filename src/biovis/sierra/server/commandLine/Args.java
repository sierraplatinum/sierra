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
package biovis.sierra.server.commandLine;

/**
 *
 * @author Lydia Mueller
 */
public class Args {
	private final static String help_message = "java -jar Peakme.jar -d <data list> -m <data mapping> -p <peak dir>\n"
			+ "                    [-w windowsize -o offset][-b -j <joind distance>]\n"
			+ "                    [-t <threshold>] [-s]\n"
			+ "	                   [-C sierra config -S sierra state -D sierra data mapper]"
			+ "\n"
			+ "options:\n"
			+ "\td  file containing a name and the path to the sam or bam file for each control and experiment.\n"
			+ "\t   It is assumed that bam/sam files contain tags only (i.e. PCR duplicates are removed with e.g.\n"
			+ "\t   picard tools.)\n"
			+ "\tm  file with mapping first column name of experiment, second column name of control. Names have to\n"
			+ "\t   occure in data file (-d option).\n"
			+ "\tp  directory to ouput peaks as bed files and csv files\n"
			+ "\tw  window size for peak calculation (specify also -o) [default: 400]\n"
			+ "\to  offset for sliding windows for peak calculation (specify also -w) [default: 100]\n"
			+ "\tb  output also broad peaks\n"
			+ "\tj  maximal distance between narrow peaks to join to broad peaks (requires -b) [default: 800]\n"
			+ "\tt  p-value threshold to determine peak location [default: 1e-5]\n"
			+ "\ts  also output peaks for each replicate (single peak calls)\n\n";
	private String datalist;
	private String datamap;
	private String peakdir;
	private Integer windowSize;
	private Integer offset;
	private Boolean broad;
	private Integer joinDist;
	private Double threshold;
	private String errors;
	private Boolean single;
	private String config;
	private String state;
	private String datamapper;

        /**
         * Constructor.
         */
	public Args(){
		datalist = "";
		datamap = "";
		peakdir = "";
		windowSize = 400;
		offset = 100;
		broad = false;
		joinDist = 800;
		threshold = 1e-5;
		errors = "";
		single = false;
		config = "";
		state = "";
		datamapper = "";
	}

        /** Parse command line arguments
         *
         * @param args  command line arguments
         */
	public void parse(String[] args){
		if(args.length == 0){
			errors += "[ERROR] No Arguments given: required arguemnts are -d, -m, and -p";
		}
		for(int i = 0; i < args.length; i++){
			switch(args[i]){
			case "-d":
				i++;
				if(i < args.length){
					datalist = args[i];
				}else{
					errors += "[ERROR] no arguemnt given for option -d\n";
				}
				break;
			case "-m":
				i++;
				if(i < args.length){
					datamap = args[i];
				}else{
					errors += "[ERROR] no arguemnt given for option -m\n";
				}
				break;
			case "-p":
				i++;
				if(i < args.length){
					peakdir = args[i];
				}else{
					errors += "[ERROR] no arguemnt given for option -p\n";
				}
				break;
			case "-w":
				i++;
				if(i < args.length){
					windowSize = Integer.parseInt(args[i]);
				}else{
					errors += "[ERROR] no argument given for option -w\n";
				}
				break;
			case "-o":
				i++;			if(i < args.length){
					offset = Integer.parseInt(args[i]);
				}else{
					errors += "[ERROR] no argument given for option -o\n";
				}
				break;
			case "-j":
				i++;
				if(i < args.length){
					joinDist = Integer.parseInt(args[i]);
				}else{
					errors += "[ERROR] no argument given for option -j\n";
				}
				break;
			case "-b":
				broad = true;
				break;
			case "-t":
				i++;
				if(i < args.length){
					threshold = Double.parseDouble(args[i]);
				}else{
					errors += "[ERROR] no argument given for option -t\n";
				}
				break;
			case "-s":
				single = true;
				break;
			case "-C":
				i++;
				if(i < args.length){
					config = args[i];
				}else{
					errors += "[ERROR] no arguemnt given for option -C\n";
				}
				break;
			case "-S":
				i++;
				if(i < args.length){
					state = args[i];
				}else{
					errors += "[ERROR] no arguemnt given for option -S\n";
				}
				break;
			case "-D":
				i++;
				if(i < args.length){
					datamapper = args[i];
				}else{
					errors += "[ERROR] no arguemnt given for option -D\n";
				}
				break;
			default:
				errors += "[ERROR] un-recognized option "+args[i]+"\n";

			}

		}
	}

	public String getHelp_message() {
		return help_message;
	}

	public String getDatalist() {
		return datalist;
	}

	public String getDatamap() {
		return datamap;
	}

	public String getPeakdir() {
		return peakdir;
	}

	public Integer getWindowSize() {
		return windowSize;
	}

	public Integer getOffset() {
		return offset;
	}

	public Boolean getBroad() {
		return broad;
	}

	public Integer getJoinDist() {
		return joinDist;
	}

	public Double getThreshold() {
		return threshold;
	}

	public String getErrors() {
		return errors;
	}

	public Boolean getSingle() {
		return single;
	}

	public String getConfig() {
		return config;
	}

	public String getState() {
		return state;
	}

	public String getDatamapper() {
		return datamapper;
	}
}
