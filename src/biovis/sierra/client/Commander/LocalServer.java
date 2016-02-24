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

import de.kl.vis.lib.remoteControl.CommandListenerException;
import biovis.sierra.server.Commander.SierraServer;
/**
*
* @author Daniel Gerighausen
*/
public class LocalServer {

	private static Thread server = new Thread();

	public static void startLocalServer()
	{	
			server = new Thread(new Runnable()
			{
				@Override
				public void run() {

					System.out.println("starting local server");
					try {
						SierraServer.main(new String[0]);
					} catch (CommandListenerException e) {
						System.err.println("Exception received!");
						return;
						
					}
				}
			});

			server.start();
			try {
				server.join(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			System.err.println("This thread is running?" +isRunning());		
		}

	public static boolean isRunning()
	{
		return server.isAlive();

	}
}
