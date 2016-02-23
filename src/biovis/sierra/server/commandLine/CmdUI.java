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

import java.util.logging.Logger;

/**
 *
 * @author Lydia Mueller
 */
public class CmdUI {

	public static void main(String[] args) {
                Logger logger = Logger.getLogger("CmdUI.main");
		Args argsParser = new Args();
		argsParser.parse(args);

                /*
		String errors = argsParser.getErrors();
		if(errors.contains("ERROR")){
                        logger.severe(errors);
                        logger.severe(argsParser.getHelp_message());
			return;
		}
		if(!argsParser.getConfig().equals("")){
			DataMapper dm = new DataMapper();
			Importer.loadConfig(argsParser.getConfig(), dm);
			SuperDuperPeakCaller peakcaller = new SuperDuperPeakCaller(dm, null);
			try {
				peakcaller.start();
//				Exporter.exportWindows(argsParser.getState(), peakcaller.getWl(), dm);
				Exporter.exportMapper(argsParser.getDatamapper(), dm);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return;
		}

		try {
			//do window-wise statistics
			SuperDuperPeakCaller peakcaller = new SuperDuperPeakCaller(argsParser.getDatalist(), argsParser.getDatamap(), argsParser.getWindowSize(), argsParser.getOffset());
			PeakFactory pf = peakcaller.start();
                        logger.info("PeakFactory generated");
			//generate peak list for narrow peaks
			pf.setThreshold(argsParser.getThreshold());
			PeakList narrow = pf.getNarrowPeakList();
                        logger.info("Narrow peak list generated");
			Exporter.exportBED(argsParser.getPeakdir()+File.separator+"narrow.bed", narrow, "narrow");
			Exporter.exportCSV(argsParser.getPeakdir()+File.separator+"narrow.csv", narrow);
                        logger.info("Narrow peak list exported");
			if(argsParser.getBroad()){
				//generate broad peak list
				PeakList broad;

				if(pf.getMaxDistance() != argsParser.getJoinDist()){
					broad= pf.getBroadPeakListWithDistance(argsParser.getJoinDist());
				}else{
					broad= pf.getBroadPeakList();
				}
                                logger.info("Broad peak list generated");
				Exporter.exportBED(argsParser.getPeakdir()+File.separator+"broad.bed", broad, "broad");
				Exporter.exportCSV(argsParser.getPeakdir()+File.separator+"broad.csv", broad);
                                logger.info("Broad peak list exported");
			}
			//single peak calls
			if(argsParser.getSingle()){
				DataMapper dm = peakcaller.getMapper();
				for(Replicate  rep : dm.getReplicates()){
					narrow = pf.getNarrowPeakList(rep.getExperiment().getIndex());
					Exporter.exportBED(argsParser.getPeakdir()+File.separator+rep.getTag()+"_narrow.bed", narrow, "narrow");
					Exporter.exportCSV(argsParser.getPeakdir()+File.separator+rep.getTag()+"_narrow.csv", narrow);
					if(argsParser.getBroad()){
						//generate broad peak list
						PeakList broad;
						if(pf.getMaxDistance() != argsParser.getJoinDist()){
							broad= pf.getBroadPeakListWithDistance(argsParser.getJoinDist(),rep.getExperiment().getIndex());
						}else{
							broad= pf.getBroadPeakList(rep.getExperiment().getIndex());
						}
						Exporter.exportBED(argsParser.getPeakdir()+File.separator+rep.getTag()+"_broad.bed", broad, "broad");
						Exporter.exportCSV(argsParser.getPeakdir()+File.separator+rep.getTag()+"_broad.csv", broad);
					}
				}
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not open input files: {0}\n{1}", new Object[]{e.getMessage(), e.getStackTrace()});
		} catch (NoFileForTagException e) {
			logger.log(Level.SEVERE, "Missing input file for name: {0}\n{1}", new Object[]{e.getMessage(), e.getStackTrace()});
		}
                */
	}
}
