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
package biovis.sierra.data.IO;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import biovis.sierra.client.GUI.FXMLDocumentController;
import biovis.sierra.data.ConfigObject;
import biovis.sierra.data.ConfigObject.SmallReplicate;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovis.sierra.server.SuperDuperPeakCaller;
import biovis.sierra.server.Commander.PeakCommander;
import biovis.sierra.server.Commander.ServerMapper;

import com.google.gson.Gson;

/**
 *
 * @author Daniel Gerighausen
 */
public class Importer {

	/**
	 * Load configuration from file.
	 *
	 * @param path path to file
	 * @param mapper data mapper to load
	 */
	public static void loadConfig(String path, DataMapper mapper)
	{
		Gson gson = new Gson();

		try (
				FileInputStream fis = new FileInputStream(path);
				GZIPInputStream gzip = new GZIPInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				) {
			String content;

			content = br.readLine();

			ConfigObject config = gson.fromJson(content, ConfigObject.class);

			//            System.err.println(config.getOffset());
			mapper.setOffset(config.getOffset());
			mapper.setPvaluecutoff(config.getPvaluecutoff());
			mapper.setWindowsize(config.getWindowsize());
			mapper.setNumCores(config.getNumCores());
			mapper.setJobName(config.getJobName());

			for(SmallReplicate srep : config.getReplicates())
			{
				//System.err.println(srep.getExperiment());
				//System.err.println(srep.getBackground());
				Replicate rep = new Replicate(srep.getName());
				rep.getExperiment().setDescription(srep.getExperiment());
				rep.getBackground().setDescription(srep.getBackground());
				mapper.addReplicate(rep);
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}



	public static void loadServerMapper(String path, ServerMapper sMapper)
	{
		Gson gson = new Gson();

		try (
				FileInputStream fis = new FileInputStream(path);
				GZIPInputStream gzip = new GZIPInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				) {
			String content;

			content = br.readLine();

			ServerMapper config = gson.fromJson(content, ServerMapper.class);

			sMapper.setChunksize(config.getChunksize());
			sMapper.setMapper(config.getMapper());
			sMapper.setPasswd(config.getPasswd());
			config = null;


		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	/**
	 * Load state from file, client.
	 *
	 * @param path path of file
	 * @param controller controller to fill
	 */
	public static void loadState(String path, FXMLDocumentController controller)
	{
		Gson gson = new Gson();

		try (
				FileInputStream fis = new FileInputStream(path);
				GZIPInputStream gzip = new GZIPInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				) {
			String content;

			content = br.readLine();

			DataMapper mapper = gson.fromJson(content, DataMapper.class);
			controller.setDataMapper(mapper);
			WindowList wList = new WindowList();

			while ((content = br.readLine()) != null)
			{
				Window w = gson.fromJson(content, Window.class);
				wList.addWindow(w);
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	/**
	 * Load state from file, server.
	 *
	 * @param path path to file
	 * @param controller controller to fill
	 * @param pc peak commander
	 */
	public static void loadState(String path, ServerMapper controller, PeakCommander pc)
	{
		Gson gson = new Gson();

		try (
				FileInputStream fis = new FileInputStream(path);
				GZIPInputStream gzip = new GZIPInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				) {
			String content;

			content = br.readLine();

			DataMapper mapper = gson.fromJson(content, DataMapper.class);
			controller.setMapper(mapper);
			WindowList wList = new WindowList();

			while ((content = br.readLine()) != null)
			{
				//				   System.out.println(content);
				Window w = gson.fromJson(content, Window.class);
				w.initAfterImport();
				wList.addWindow(w);
				
			}
			wList.init();

			//			controller.setWindowList(wList);
			controller.setSpc(new SuperDuperPeakCaller(controller.getMapper(), controller.getChunksize()));
			controller.getSpc().setWl(wList);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	/**
	 * Load data mapper, client.
	 *
	 * @param path path of file
	 * @param controller controller
	 */
	public static void loadMapper(String path, FXMLDocumentController controller)
	{
		Gson gson = new Gson();
		try (
				FileInputStream fis = new FileInputStream(path);
				GZIPInputStream gzis = new GZIPInputStream(fis);
				//
				OutputStream out = new ByteArrayOutputStream();
				) {
			byte[] buf = new byte[1024];
			int len;
			while ((len = gzis.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			String res = out.toString();
			//			System.err.println(res);
			DataMapper obj = gson.fromJson(res, DataMapper.class);

			//			System.err.println(obj.toString());
			controller.setDataMapper(obj);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
}
