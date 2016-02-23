/*******************************************************************************
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
 *******************************************************************************/
package biovis.sierra.server.Commander;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.IO.Importer;
import biovis.sierra.server.SuperDuperPeakCaller;
import de.kl.vis.lib.remoteControl.CommandListener;
import de.kl.vis.lib.remoteControl.CommandListenerException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.*;
/**
 *
 * @author Daniel Gerighausen
 */
public class SierraService {
	private ServerMapper sm;
	private CommandListener commandListener;
	private PeakDispatcher peakdisp;
	private int port;

	/**
	 * Constructor.
	 */
	public SierraService()
	{

	}

	/**
	 * Init server.
	 *
	 * @param args command line arguments
	 * @throws CommandListenerException in case of problems
	 */
	public  void initServer(String[] args) throws CommandListenerException
	{
		Logger logger = Logger.getLogger("SierraService.initServer");
		sm = new ServerMapper();
		peakdisp = new PeakDispatcher();
		Args argsParser = new Args();
		argsParser.parse(args);
		peakdisp.setServer(sm);

		String errors = argsParser.getErrors();
		if(errors.contains("ERROR")){
			logger.severe(errors);
			logger.severe(argsParser.getHelp_message());
			return;
		}
		if(argsParser.getChunkSize() != 10000){
			sm.setChunksize(argsParser.getChunkSize());
		}
		if(argsParser.getIOThreads() != 6){

			sm.setIOThreads(argsParser.getIOThreads());
		}
		if(!argsParser.getState().equals("")){
			Importer.loadState(argsParser.getState(), sm, peakdisp.getPc());
			sm.getSpc().init();
		}
		if(!argsParser.getServerConfig().equals("")){
			Importer.loadServerMapper(argsParser.getServerConfig(), sm);
			sm.setSpc(new SuperDuperPeakCaller(sm.getMapper(),peakdisp.getPc(),sm.getChunksize()));
			//			sm.getSpc().init();
		}
		if(!argsParser.getClientConfig().equals("")){

			Importer.loadConfig(argsParser.getClientConfig(), sm.getMapper());
			DataMapper dm = sm.getMapper();
			if (dm.getJobName() == null) {
				String config = argsParser.getClientConfig();
				System.err.println("config: " + config);
				dm.setJobName(config.substring(0, config.indexOf(".")));
			}
			sm.setSpc(new SuperDuperPeakCaller(sm.getMapper(),peakdisp.getPc(),sm.getChunksize()));
			if(argsParser.getIOThreads() != 6){

				dm.setIOThreads(argsParser.getIOThreads());
				
			}
			//			sm.getSpc().init();
			if(argsParser.getBatch())
			{
				System.err.println("Exit Sierra Server after calculating the given client config");
				peakdisp.setBatch(argsParser.getBatch());
			}
			peakdisp.startCalc();

		}


		port = argsParser.getPort();
		startListener();




	}


	private void startListener() throws CommandListenerException
	{
		Logger logger = Logger.getLogger("SierraService.startListener");
		//Datamapper erweitern
		while(true)
		{
			try {
				System.err.println("Restart");
				commandListener = new CommandListener(port, peakdisp);
				commandListener.start();
			} catch (CommandListenerException cle) {
				logger.severe("Error while creating commandListener"); //$NON-NLS-1$
				//			System.exit(0);
				throw new CommandListenerException("Error while creating commandListener");
			}
			logger.info("Server Command listener started"); //$NON-NLS-1$
			try {
				commandListener.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}




}
