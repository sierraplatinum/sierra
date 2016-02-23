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
package biovis.sierra.server.Commander;

/**
 *
 * @author Daniel Gerighausen, Lydia Mueller
 */
public class Args {
	private final static String
                help_message = "java -jar sierraServer.jar "
			+ "\n"
			+ "options:\n"
			+ "\t-port  port of the server to listen for new connections [default: 9753]\n"
			+ "\t-state  load a precalculated state for further calculations\n"
			+ "\t-serverConfig  load a config file for the server parameters\n"
			+ "\t-batch terminate the program after the calculation\n"
			+ "\t-IOThreads the amount threads that is used during the IO intensive calculations [default: 6]\n"
			+ "\t-clientConfig  load a GUI config file for the server and start the calculation without a client\n"
			+ "\t-chunkSize  chunk size for the calculation [default: 1000]\n";

	private String errors;
	private String serverConfig;
	private String state;
	private String clientConfig;
	private Integer port;
	private Integer chunkSize;
	private int ioThreads;
	private boolean batch;



	/**
	 * Constructor.
	 */
	public Args(){
		errors = "";
		serverConfig = "";
		state = "";
		clientConfig = "";
		port = 9753;
		chunkSize = 1000;
		ioThreads =  6;
	}

	/** Parse command line arguments
	 *
	 * @param args  command line arguments
	 */
	public void parse(String[] args){
		//		if(args.length == 0){
		//			errors += "[ERROR] No Arguments given: required arguemnts are -d, -m, and -p";
		//		}
		for(int i = 0; i < args.length; i++){
			switch(args[i]){
			case "-chunkSize":
				i++;
				if(i < args.length){
					chunkSize = Integer.parseInt(args[i]);
				}else{
					errors += "[ERROR] no argument given for option -c\n";
				}
				break;
			case "-port":
				i++;
				if(i < args.length){
					port = Integer.parseInt(args[i]);
				}else{
					errors += "[ERROR] no argument given for option -p\n";
				}
				break;
			case "-serverConfig":
				i++;
				if(i < args.length){
					serverConfig = args[i];
				}else{
					errors += "[ERROR] no argument given for option -l\n";
				}
				break;
			case "-clientConfig":
				i++;
				if(i < args.length){
					clientConfig = args[i];
				}else{
					errors += "[ERROR] no argument given for option -d\n";
				}
				break;
			case "-batch":
				
				
				batch = true;
				break;
			case "-state":
				i++;
				if(i < args.length){
					state = args[i];
				}else{
					errors += "[ERROR] no argument given for option -S\n";
				}
				break;
			case "-IOThreads":
				i++;
				if(i < args.length){
					ioThreads = Integer.parseInt(args[i]);
				}else{
					errors += "[ERROR] no argument given for option -S\n";
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

	public Integer getPort() {
		return port;
	}

	public Integer getChunkSize() {
		return chunkSize;
	}

	public String getServerConfig() {
		return serverConfig;
	}

	public String getClientConfig() {
		return clientConfig;
	}

	public String getState() {
		return state;
	}
	public boolean getBatch()
	{
		return batch;
	}
	public String getErrors() {
		return errors;
	}
	public int getIOThreads()
	{
		return ioThreads;
	}
}
