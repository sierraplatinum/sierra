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

import de.kl.vis.lib.remoteControl.CommandListenerException;

//import java.io.File;
//import java.io.PrintStream;
/**
 *
 * @author Daniel Gerighausen
 */
public class SierraServer {

    private static SierraService server;
//	private static Memory bla;


	public static void main(String[] args) throws CommandListenerException {

//		try {
//			System.setOut(new PrintStream(new File("server.trace")));
//			System.setErr(new PrintStream(new File("server.trace")));
//		} catch (Throwable thr) {
//			System.err.println("Throwable catched: " + thr.toString()); //$NON-NLS-1$
//		}
        server = new SierraService();
//        Timer timer = new Timer();
//        bla = new Memory(server);
//        timer.schedule(bla, 0, 1000);
        server.initServer(args);
    }
}
