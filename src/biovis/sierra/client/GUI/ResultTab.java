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
package biovis.sierra.client.GUI;
import java.io.IOException;

import biovis.sierra.client.Commander.PeakCommander;
import biovis.sierra.data.DataMapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Tab;
import javafx.scene.image.WritableImage;
/**
*
* @author Daniel Gerighausen
*/
public class ResultTab {
	

	private Tab tab;
	private SummaryController controller;


	public ResultTab(String name)
	{
		tab = new Tab();
		tab.setText(name);
	}

	public Tab getTab()
	{
		return tab;
	}
	
	
	
	
	
	public void init(double[][] correlations, DataMapper mapper, PeakCommander pc)
	{
		
		 FXMLLoader loader = new FXMLLoader();
		 try {
			Parent root = (Parent) loader.load(getClass().getResourceAsStream("Summary.fxml"));
			tab.setContent(root);
		} catch (IOException e) {
			e.printStackTrace();
		}
		  controller = loader.getController();
//		 System.err.println(controller);
		  controller.printReplicates(mapper, pc);
		controller.draw(correlations);
		

		
	
	}
	
	public WritableImage exportImage()
	{
		//		System.err.println("-2");
		WritableImage snapshot = controller.getHeatMap().snapshot(new SnapshotParameters(), null);
		//		 System.err.println("-1");
		return snapshot;
	}
	public int getHeight()
	{
		return (int) controller.getHeatMap().getHeight();
	}
	public int getWidth()
	{
		return (int) controller.getHeatMap().getWidth();
	}

	public void setProgress(Double progressState) {
		controller.setProgress(progressState);		
	}

}
