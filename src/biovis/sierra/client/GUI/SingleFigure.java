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

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import biovis.sierra.client.GUI.GUIHelper.BoxPlot;
import biovis.sierra.client.GUI.GUIHelper.ChartCanvas;
import biovis.sierra.client.GUI.GUIHelper.LogarithmicAxis;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.IO.ExportSnapshot;
import biovis.sierra.data.peakcaller.IntBoxplotData;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
/**
 *
 * @author Daniel Gerighausen
 */
public class SingleFigure {


	private StackPane pane;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void createSingleFigure(ObservableList observableList ,boolean range, String title, boolean hist, boolean pvalue,int max)
	{
		final Stage myDialog = new Stage();
		myDialog.setTitle(title);
		myDialog.setHeight(768);
		myDialog.setWidth(1024);
		myDialog.initModality(Modality.WINDOW_MODAL);
		Image ico = new Image(getClass().getResource("sierra.png").toExternalForm());
		myDialog.getIcons().add(ico);
		pane = new StackPane();
		Series<Object, Object> serie1 = new Series();
		Series<Object, Object> serie2 = new Series();


		Series<Object, Object> temp = (Series<Object, Object>) observableList.get(0);

		for(int i = 0; i < temp.getData().size(); i++)
		{

			serie1.setName(temp.getName());
			serie1.getData().add(temp.getData().get(i));
		}

		temp = (Series<Object, Object>) observableList.get(1);

		for(int i = 0; i < temp.getData().size(); i++)
		{

			serie2.setName(temp.getName());
			serie2.getData().add(temp.getData().get(i));
		}
		final Axis gxAxis;
		if(hist)
		{
			gxAxis = new LogarithmicAxis(0.4, max);
		}

		else
		{
			gxAxis = new NumberAxis();
		}
		Axis  gyAxis = new NumberAxis();

		if(pvalue)
		{
			gyAxis = new LogarithmicAxis(100, max+10000);
		}
		LineChart figure = new LineChart(gxAxis, gyAxis);
		figure.getData().add(serie1);
		figure.getData().add(serie2);
		figure.setCreateSymbols(false);
		figure.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					WritableImage snapshot = figure.snapshot(new SnapshotParameters(), null);
					ExportSnapshot.exportImage(snapshot, pane);
					event.consume();
				}

			}
				}
				);


		if(range)
		{
			final double SCALE_DELTA = 1.1;




			figure.setOnScroll(new EventHandler<ScrollEvent>() {
				public void handle(ScrollEvent event) {
					event.consume();
					//		        System.err.println("bla");
					if (event.getDeltaY() == 0) {
						return;
					}
					gxAxis.setAutoRanging(false);
					double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
					((NumberAxis)gxAxis).setUpperBound(((NumberAxis)gxAxis).getUpperBound()*scaleFactor);

				}
			});

			figure.setOnMousePressed(new EventHandler<MouseEvent>() {
				public void handle(MouseEvent event) {
					if (event.getClickCount() == 2) {
						//		        	x.setUpperBound(upperNorm);
						gxAxis.setAutoRanging(true);
					}
				}
			});
		}

		pane.getChildren().add(figure);



		Scene myDialogScene = new Scene(pane);
		//		File line = new File("linechart.css");
		//      File progress = new File("progress.css");
		if(pvalue)
		{
			myDialogScene.getStylesheets().clear();
			myDialogScene.getStylesheets().add(getClass().getResource("histogram.css").toExternalForm());

		}
		else
		{
			myDialogScene.getStylesheets().clear();
			myDialogScene.getStylesheets().add(getClass().getResource("linechart.css").toExternalForm());
		}

		myDialog.setScene(myDialogScene);
		myDialog.show();
	}




	public void createBoxPlot(String string, String replicate, Replicate rep)
	{
		final BoxPlot demo = new BoxPlot("Tag quality", replicate, rep);
		demo.setVisible(true);
		demo.setBackground(new Color(244,244,244));
		ChartCanvas canvas = new ChartCanvas(demo.chart);

		final Stage myDialog = new Stage();
		myDialog.setTitle(replicate);
		myDialog.setHeight(768);
		myDialog.setWidth(1024);
		myDialog.initModality(Modality.WINDOW_MODAL);
		//		URL picture = getClass().getResource("sierra.png");

		Image ico = new Image(getClass().getResource("sierra.png").toExternalForm());
		myDialog.getIcons().add(ico);

		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);
					ExportSnapshot.exportImage(snapshot, pane);
					event.consume();
				}

			}
				}
				);


		pane = new StackPane();
		//		pane.getChildren().add(new Label("foo"));
		Scene myDialogScene = new Scene(pane);


		canvas.widthProperty().bind( pane.widthProperty()); 
		canvas.heightProperty().bind(pane.heightProperty()); 

		pane.getChildren().add(canvas);

		//		File line = new File("linechart.css");
		//      File progress = new File("progress.css");
		myDialogScene.getStylesheets().clear();
		myDialogScene.getStylesheets().add(getClass().getResource("linechart.css").toExternalForm());  

		myDialog.setScene(myDialogScene);
		myDialog.show();
	}


	public void createSingleBarChart(Replicate rep, DataMapper mapper, String title, String name, String type, int max)
	{
		final Stage myDialog = new Stage();
		myDialog.setTitle(title);
		myDialog.setHeight(768);
		myDialog.setWidth(1024);
		myDialog.initModality(Modality.WINDOW_MODAL);
		Image ico = new Image(getClass().getResource("sierra.png").toExternalForm());
		myDialog.getIcons().add(ico);
		pane = new StackPane();
		Series<String, Integer> serie1 = null;
		Series<String, Integer> serie2 = null;

		final CategoryAxis gxAxis = new CategoryAxis();

		Axis gyAxis;// = new NumberAxis();


		gyAxis = new NumberAxis();
		
		
		
		if(type.equals("pValueHist"))
		{
		gyAxis = new LogarithmicAxis(0.4,max);
		}
		BarChart figure = new BarChart(gxAxis, gyAxis);
		if(type.equals("sigHist"))
		{

		 serie1= biovis.sierra.client.GUI.GUIHelper.BarChart.getSigHist(rep, name);

		serie2 = biovis.sierra.client.GUI.GUIHelper.BarChart.getBarChart( mapper, "Median");
		figure.setCategoryGap(2);
		figure.setBarGap(0);
		figure.getData().add(serie1);
		figure.getData().add(serie2);


		for (final Data data : serie1.getData()) {
			Tooltip tooltip = new Tooltip();
			tooltip.setText(data.getYValue().toString());
			Tooltip.install(data.getNode(), tooltip);                    
		}
		for (final Data data : serie2.getData()) {
			Tooltip tooltip = new Tooltip();
			tooltip.setText(data.getYValue().toString());
			Tooltip.install(data.getNode(), tooltip);                    
		}

		//		figure.
		//		figure.getData().add(serie2);
		

		}
		if(type.equals("pValueHist"))
		{
		
			biovis.sierra.client.GUI.GUIHelper.BarChart.getPValueHist(rep, name, mapper, figure);
		}





	

	
		figure.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					WritableImage snapshot = figure.snapshot(new SnapshotParameters(), null);
					ExportSnapshot.exportImage(snapshot, pane);
					event.consume();
				}

			}
				}
				);




		pane.getChildren().add(figure);



		Scene myDialogScene = new Scene(pane);

		myDialog.setScene(myDialogScene);
		myDialog.show();
	}

	public void createBoxPlot(String string, String replicate, IntBoxplotData experimentData, IntBoxplotData backgroundData)
	{
		final BoxPlot demo = new BoxPlot("Tag quality", replicate, experimentData, backgroundData);
		demo.setVisible(true);
		demo.setBackground(new Color(244,244,244));
		ChartCanvas canvas = new ChartCanvas(demo.chart);

		final Stage myDialog = new Stage();
		myDialog.setTitle(replicate);
		myDialog.setHeight(768);
		myDialog.setWidth(1024);
		myDialog.initModality(Modality.WINDOW_MODAL);
		//		URL picture = getClass().getResource("sierra.png");

		Image ico = new Image(getClass().getResource("sierra.png").toExternalForm());
		myDialog.getIcons().add(ico);

		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);
					ExportSnapshot.exportImage(snapshot, pane);
					event.consume();
				}

			}
				}
				);


		pane = new StackPane();
		//		pane.getChildren().add(new Label("foo"));
		Scene myDialogScene = new Scene(pane);


		canvas.widthProperty().bind( pane.widthProperty()); 
		canvas.heightProperty().bind(pane.heightProperty()); 

		pane.getChildren().add(canvas);

		//		File line = new File("linechart.css");
		//      File progress = new File("progress.css");
		myDialogScene.getStylesheets().clear();
		myDialogScene.getStylesheets().add(getClass().getResource("linechart.css").toExternalForm());  

		myDialog.setScene(myDialogScene);
		myDialog.show();
	}


	public void createSingleBarChart(DataMapper mapper , String title, boolean logScale, int max)
	{
		final Stage myDialog = new Stage();
		myDialog.setTitle(title);
		myDialog.setHeight(768);
		myDialog.setWidth(1024);
		myDialog.initModality(Modality.WINDOW_MODAL);
		Image ico = new Image(getClass().getResource("sierra.png").toExternalForm());
		myDialog.getIcons().add(ico);
		pane = new StackPane();
		


		final CategoryAxis gxAxis = new CategoryAxis();

		final Axis gyAxis;// = new NumberAxis();
		if(logScale)
		{
			gyAxis = new LogarithmicAxis(0.4,max);
			gyAxis.setTickMarkVisible(false);
		}
		else
		{
			gyAxis = new NumberAxis();
		}

		BarChart figure = new BarChart(gxAxis, gyAxis);
		figure.setCategoryGap(2);
		figure.setBarGap(0);


		if(logScale)
		{
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

				figure.getData().add(pvalues);
				for (final Data data : pvalues.getData()) {
					Tooltip tooltip = new Tooltip();
					tooltip.setText(data.getYValue().toString());
					Tooltip.install(data.getNode(), tooltip);                    
				}
			}
//			for(Integer run : mapper.getFinalPValueExp().keySet())
//			{
//				if(start > run)
//				{
//					continue;
//				}
//				Series<String, Integer> pvalues = new XYChart.Series<>();
//
//				for(int i = 0; i < mapper.getFinalPValueExp().get(run).size(); i++)
//				{
//					pvalues.getData().add(
//							new XYChart.Data<>(
//									String.valueOf(mapper.getFinalPValueExp().get(run).get(i).getFirst()),
//									mapper.getFinalPValueExp().get(run).get(i).getSecond()));
//					if(max < mapper.getFinalPValueExp().get(run).get(i).getSecond())
//					{
//						max = mapper.getFinalPValueExp().get(run).get(i).getSecond();
//					}
//					figure.getData().add(pvalues);
//					for (final Data data : pvalues.getData()) {
//						Tooltip tooltip = new Tooltip();
//						tooltip.setText(data.getYValue().toString());
//						Tooltip.install(data.getNode(), tooltip);                    
//					}
//				}
//			}
		}
		else
		{
			int start = mapper.getFinalPValueExp().keySet().size()-7;
			for(Integer run : mapper.getOverlapWithReplicates().keySet())
			{
				if(start > run)
				{
					continue;
				}
				Series<String, Double> serie1 = new XYChart.Series<>();

				serie1.setName("Run " + run);
				List<Double> values = new ArrayList<>(mapper.getOverlapWithReplicates().get(run).values());

				List<Integer> keys = new ArrayList<>(mapper.getOverlapWithReplicates().get(run).keySet());
				for(int i = 0 ; i < values.size(); i++)
				{
					serie1.getData().add(new   XYChart.Data<>(mapper.getReplicates().get(i).getName(), values.get(i)));
				}
				figure.getData().add(serie1);
				for (final Data data : serie1.getData()) {
					Tooltip tooltip = new Tooltip();
					tooltip.setText(data.getYValue().toString());
					Tooltip.install(data.getNode(), tooltip);                    
				}
			}

		}


		


	

		//		figure.
		//		figure.getData().add(serie2);
		figure.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					WritableImage snapshot = figure.snapshot(new SnapshotParameters(), null);
					ExportSnapshot.exportImage(snapshot, pane);
					event.consume();
				}

			}
				}
				);




		pane.getChildren().add(figure);



		Scene myDialogScene = new Scene(pane);
		//		File line = new File("linechart.css");
		//      File progress = new File("progress.css");
		//				myDialogScene.getStylesheets().clear();
		//				myDialogScene.getStylesheets().add(getClass().getResource("histogram.css").toExternalForm());

		myDialog.setScene(myDialogScene);
		myDialog.show();
	}



}
