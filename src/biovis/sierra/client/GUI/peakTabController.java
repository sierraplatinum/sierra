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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import biovis.sierra.client.GUI.GUIHelper.LogarithmicAxis;
import biovis.sierra.data.DataMapper;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
/**
 *
 * @author Daniel Gerighausen
 */
public class peakTabController implements Initializable 
{

	@FXML
	private GridPane chartPane;
	@FXML
	private BarChart<String, Double> histogram;
	private BarChart<String, Integer> pvalueChart;
	//	
	//	@FXML
	//	private NumberAxis pxAxis = new NumberAxis();
	//	@FXML
	//	private NumberAxis pyAxis = new NumberAxis();
	//	@FXML
	//	private NumberAxis hxAxis = new NumberAxis();
	//	@FXML
	//	private NumberAxis hyAxis = new NumberAxis();
	@FXML
	private StackPane logPane;
	private int max;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}


	@SuppressWarnings("unchecked")
	public void drawPlots(DataMapper mapper) {

		@SuppressWarnings("rawtypes")
		Axis logaxis = new LogarithmicAxis(0.4,mapper.getMaxPValueExp());
		logaxis.setTickMarkVisible(false);
		//		logaxis.set
		CategoryAxis catAxis = new CategoryAxis();
		pvalueChart = new BarChart<String, Integer>(catAxis, logaxis);

		pvalueChart.setPrefSize(800, 600);
		pvalueChart.setMaxSize(800, 600);
		pvalueChart.setTitle("Final p-value histogram");
		logPane.getChildren().add(pvalueChart);
		pvalueChart.setCategoryGap(2);
		pvalueChart.setBarGap(0);
		
		System.err.println("Number of runs: "+mapper.getFinalPValueExp().keySet().size());
		int start = mapper.getFinalPValueExp().keySet().size()-7;
		for(Integer run : mapper.getFinalPValueExp().keySet())
		{
			if(start > run)
			{
				continue;
			}
			Series<String, Integer> pvalues = new XYChart.Series<>();
			max = 0;
			for(int i = 0; i < mapper.getFinalPValueExp().get(run).size(); i++)
			{
				pvalues.getData().add(
						new XYChart.Data<>(
								String.valueOf(mapper.getFinalPValueExp().get(run).get(i).getFirst()),
								mapper.getFinalPValueExp().get(run).get(i).getSecond()));
				if(max < mapper.getFinalPValueExp().get(run).get(i).getSecond())
				{
					max = mapper.getFinalPValueExp().get(run).get(i).getSecond();
				}
			}
			pvalues.setName("Run " + run + " - Max-value: "+ max);

			pvalueChart.getData().add(pvalues);
			for (final Data data : pvalues.getData()) {
				Tooltip tooltip = new Tooltip();
				tooltip.setText(data.getYValue().toString());
				Tooltip.install(data.getNode(), tooltip);                    
			}
		}
		histogram.setCategoryGap(2);
		histogram.setBarGap(0);

		for(Integer run : mapper.getOverlapWithReplicates().keySet())
		{
			if(start > run)
			{
				continue;
			}

			XYChart.Series<String, Double> hvalues = new XYChart.Series<>();
			hvalues.setName("Run " + run);
			List<Double> values = new ArrayList<>(mapper.getOverlapWithReplicates().get(run).values());

			List<Integer> keys = new ArrayList<>(mapper.getOverlapWithReplicates().get(run).keySet());
			for(int i = 0 ; i < values.size(); i++)
			{
				hvalues.getData().add(new   XYChart.Data<>(mapper.getReplicates().get(i).getName(), values.get(i)));
			}



			histogram.getData().add(hvalues);
			for (final Data data : hvalues.getData()) {
				Tooltip tooltip = new Tooltip();
				tooltip.setText(data.getYValue().toString());
				Tooltip.install(data.getNode(), tooltip);                    
			}
		}
		addListener(mapper);
	}


	private void addListener(DataMapper mapper)
	{

		pvalueChart.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					//					System.out.println("consuming right release button in cm filter");
					//					WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
					//					exportImage(snapshot);
					SingleFigure figure = new SingleFigure();
					figure.createSingleBarChart(mapper, pvalueChart.getTitle(),true, max);
					//					System.err.println(chart.getData().);
					//					System.err.println(x.hashCode());

					event.consume();
				}

			}
				}
				);

		histogram.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					//					System.out.println("consuming right release button in cm filter");
					//					WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
					//					exportImage(snapshot);
					SingleFigure figure = new SingleFigure();
					figure.createSingleBarChart(mapper, histogram.getTitle(),false, max);
					//					System.err.println(chart.getData().);
					//					System.err.println(x.hashCode());

					event.consume();
				}

			}
				}
				);



	}
	public  WritableImage exportImage()
	{
		//		System.err.println("-2");
		WritableImage snapshot = chartPane.snapshot(new SnapshotParameters(), null);
		//		 System.err.println("-1");
		return snapshot;
	}
	public int getHeight()
	{
		return (int) chartPane.getHeight();
	}
	public int getWidth()
	{
		return (int) chartPane.getWidth();
	}
}
