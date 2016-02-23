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
package biovis.sierra.client.Commander;

import java.io.File;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import biovis.sierra.client.GUI.BusyWarning;
import biovis.sierra.client.GUI.FXMLDocumentController;
import biovis.sierra.client.GUI.ServerDialog;
import biovis.sierra.client.GUI.StateInfo;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.IO.Exporter;
import biovis.sierra.data.peakcaller.PeakList;
import de.kl.vis.lib.remoteControl.CommandDispatcherInterface;
/**
*
* @author Daniel Gerighausen
*/
public class PeakDispatcher implements CommandDispatcherInterface {


	private FXMLDocumentController controller;
	public PeakDispatcher(FXMLDocumentController controller) {
		this.controller = controller;
	}



	@Override
	public void dispatchCommand(Object[] command) {
		String scommand = (String) command[0];

		if (scommand.equals("sendDataMapper")) {
			// Load data
//			System.err.println("sendDataMapper");
			reloadDataMapper(command);
		} else if (scommand.equals("QUIT")) { 

			dispatchQuit(command);
			System.err.println("Exit");
		}
		else if (scommand.equals("ECHO"))
		{
			System.out.println("ECHO");
		}
		else if (scommand.equals("setProgress"))
		{
//			System.out.println("Progress Update");
			dispatchProgress(command);
		}
		else if (scommand.equals("sendBroad"))
		{
			getPeaks(command);
		}
		else if (scommand.equals("sendNarrow"))
		{
			getPeaks(command);
		}
		else if (scommand.equals("BUSY"))
		{
//			System.err.println("Server is busy");
			dispatchBusy();
		}
		else if (scommand.equals("killedJob"))
		{
			System.err.println("Server killed running task");
			dispatchKilled();
		}
		else if (scommand.equals("STATE DONE"))
		{
			System.err.println("State export was successfull");
			dispatchStateDone();
		}
		else
		{
			System.err.println("Unknown command received: "+ scommand);
		}

	} 

	private void dispatchStateDone() {

		 new StateInfo().init();
		 
	
		
		
	}



	private void dispatchKilled() {
		// TODO Auto-generated method stub

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
		DataMapper temp = controller.getDataMapper();
		controller.init();
		controller.setDataMapper(temp);
		controller.mapperUpdate();
		 Platform.runLater(new Runnable() {
				@Override
				public void run() {
					 controller.setBusy(false);
				}});
		
			}});
	}



	private void dispatchBusy()
	{


		 new BusyWarning().init();
		 
		 Platform.runLater(new Runnable() {
				@Override
				public void run() {
					 controller.setBusy(true);
				}});
		
		
	}


	private void dispatchProgress(Object[] command) {

	 
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Double progress = (Double) command[1];

				controller.setProgress(progress);

				//				
			}
		});

	}



	private void dispatchQuit(Object[] command) {

		System.err.println("QUIT");
		String reason = (String) command[1];
		if(reason.equals("EXCEPTION"))
		{
			//	controller.getPeakCommander().initCommander();
			//			

			System.err.println("Restarted Listener");

		}
		else if(reason.equals("AUTH_FAILURE"))
		{

			System.err.println("AUTH FAILURE");

			controller.getPeakCommander().stopListener();
			controller.getPeakCommander().setActive(false);
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ServerDialog reconnect= new ServerDialog();
					reconnect.createServerDialog(controller);
				}
			});
		}


	}
	private void getPeaks(Object[] command)
	{
		controller.getReceiver().close();
		System.err.println("got peaks");
		Platform.runLater(new Runnable() {
			@Override
			public void run() {


				Gson gson = new Gson();
				PeakList obj = gson.fromJson( (String) command[1], PeakList.class);
				FileChooser fileChooser = new FileChooser();

				//Set extension filter
				FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("bed files (*.bed", "*.bed");
				fileChooser.getExtensionFilters().add(extFilter);
				File file;
				fileChooser.setTitle("Save peaks as bed file");
				file = fileChooser.showSaveDialog(null);
				if (file != null) {


					String path = file.getAbsolutePath();
					if(!path.endsWith(".bed"))
					{
						path +=".bed";
					}
//					System.err.println(path);
					Exporter.exportBED(path, obj, "sierra");
				}
			}
		});


	}



	private void reloadDataMapper(Object[] command)
	{

		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				Gson gson = new Gson();
				DataMapper obj = gson.fromJson( (String) command[1], DataMapper.class);
				//				DataMapper mapper = (DataMapper) command[1];
				controller.init();
				controller.setDataMapper(obj);
				controller.mapperUpdate();
				 Platform.runLater(new Runnable() {
						@Override
						public void run() {
							 controller.setBusy(false);
						}});
				
			}
		});

	}



}
