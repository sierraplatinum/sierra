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

import java.io.IOException;

import biovis.sierra.client.GUI.FXMLDocumentController;
import biovis.sierra.client.GUI.GUIHelper.RandomDNA;
import de.kl.vis.lib.remoteControl.AddressContainer;
import de.kl.vis.lib.remoteControl.CommandListener;
import de.kl.vis.lib.remoteControl.CommandListenerException;
import de.kl.vis.lib.remoteControl.Commander;
import de.kl.vis.lib.remoteControl.CommanderException;
/**
*
* @author Daniel Gerighausen
*/
public class PeakCommander {


	private String clientHost;
	private Commander commander = null;
	private int clientPort;
	private boolean active = false;
	private CommandListener commandListener;
	private FXMLDocumentController controller;
	private int serverPort;
	private PeakDispatcher peakdisp;
	private  String hash;

	public PeakCommander(FXMLDocumentController controller)
	{
		this.controller = controller;

		hash = new RandomDNA().generateStrand();
	}
	public boolean initCommander()
	{

		System.out.println("Starte neuen Commander");
		AddressContainer host = new AddressContainer(clientHost, serverPort);

		try {
			commander = new Commander(host);
			active = true;
			return true;
		} 
		catch (CommanderException cEx) {
			commander = null;

			System.err.println("No server connection was initiated");

		}
		return false;
	}

	private void initDispatcher() 
	{

		peakdisp = new PeakDispatcher(controller);
		try {
			commandListener = new CommandListener(clientPort, peakdisp);
			commandListener.start();
		} catch (CommandListenerException cle) {
			System.err
			.println("Error while creating commandListener -> terminating."); 
			if(clientHost.equals("localhost"))
			{
				System.out.println("Set new clientPort");
				clientPort++;
				initDispatcher();
			}
			//				System.exit(0);
		}
		System.out.println("Command listener started"); 
		System.out.flush();


	}


	public boolean setServer(String url, int port, Integer clientPort)
	{
		System.err.println("Set Server");

		this.clientHost = url;
		this.serverPort = port;
		this.clientPort = clientPort;
		if(initCommander())
		{
			initDispatcher();
			Object[] command = new Object[2];
			command[0] = "setClient";
			Object[] payload = new Object[2];
			payload[0] = clientPort;
			payload[1] = hash;
			command[1] = payload;

			sendCommand(command);
			return true;
		}
		else
		{
			return false;
		}
	}


	public void sendCommand(Object command)
	{
		try {
			if(commander == null)
			{
				System.err.println("Init Commander");
				initCommander();
			}
			Object[] c = (Object[]) command;
			System.out.println("sending command " +c[0]);
			commander.executeCommand(command);



		} catch (CommanderException | IOException e) {
			System.err.println("Exception while sending command: " + e);
			System.err.println("Exception while sending command: " + e);
			active = false;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean isActive()
	{
		return active;
	}
	public void setActive(boolean active)
	{
		this.active = active;
	}
	public void ack()
	{
		Object[] c = {"QUIT","ACK"};
		sendCommand(c);
		commander = null;
		System.err.println("closed Commander");
	}
	public void stopListener()
	{
		if(commandListener != null)
		{
			commandListener.closeAll();
			commandListener =null;
		}
		if(commander != null)
		{
			commander.closeAll();
			commander = null;
		}



	}


	public String getHash()
	{
		return hash;
	}
}
