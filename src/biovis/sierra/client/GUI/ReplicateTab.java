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

import biovis.sierra.client.GUI.GUIHelper.BoxPlot;
import biovis.sierra.client.GUI.GUIHelper.ChartCanvas;
import biovis.sierra.client.GUI.GUIHelper.Histogram;
import biovis.sierra.client.GUI.GUIHelper.LogarithmicAxis;
import biovis.sierra.client.GUI.GUIHelper.Poisson;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.TagCountHistogram;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tab;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
/**
*
* @author Daniel Gerighausen
*/
public class ReplicateTab {

	//Due to JavaFX it is not possible 
	@SuppressWarnings("rawtypes")
	private LineChart norm;
	@SuppressWarnings("rawtypes")
	private LineChart raw;
	@SuppressWarnings("rawtypes")
	private LineChart histogram;
	private GridPane gPane;
	private Tab tab;
	@SuppressWarnings("rawtypes")
	private BarChart pvaluehist;
	@SuppressWarnings("rawtypes")
	private BarChart sigHist;
	private DataMapper mapper;
	private Replicate rep;
	public ReplicateTab(String name)

	{
		tab = new Tab();
		
		tab.setText(name);
	}



	public Tab createTab(String name)
	{
		tab = new Tab();
		tab.setText(name);
		return tab;

	}

	public Tab getTab()
	{
		return tab;
	}
	@SuppressWarnings("unchecked")
	public void addExperimentLambda(double lambda, double range)
	{
		
		String result = String.format("%.4f", lambda);
		Poisson pSeries = new Poisson();
		Series<Integer, Double> serie = pSeries.getPoisson(lambda, "Experiment λ = "+result, range);
		norm.getData().add(serie);
//		norm.setAnimated(false);
		norm.setCreateSymbols(false);
	
	}
	@SuppressWarnings("unchecked")
	public void addBackgroundLambda(double lambda, double range)
	{
		String result = String.format("%.4f", lambda);
		Poisson pSeries = new Poisson();
		norm.getData().add(pSeries.getPoisson(lambda, "Background λ= "+result, range));
	}



	@SuppressWarnings("unchecked")
	public void addRawExperimentLambda(double lambda, double range)
	{
		String result = String.format("%.4f", lambda);
		Poisson pSeries = new Poisson();
		Series<Integer, Double> serie = pSeries.getPoisson(lambda, "Experiment λ = "+result, range);
		raw.getData().add(serie);
//		raw.setAnimated(false);
		raw.setCreateSymbols(false);
	}
	@SuppressWarnings("unchecked")
	public void addRawBackgroundLambda(double lambda,  double range)
	{

		String result = String.format("%.4f", lambda);
		Poisson pSeries = new Poisson();
		raw.getData().add(pSeries.getPoisson(lambda, "Background λ = "+result, range));


	}


	@SuppressWarnings("unchecked")
	public void addRawExperimentHistogram(TagCountHistogram tags)
	{
		Series<Double, Double> serie = Histogram.getHistogram(tags , "Experiment histogram");
		histogram.getData().add(serie);
//		histogram.setAnimated(false);
		histogram.setCreateSymbols(false);
	}
	@SuppressWarnings("unchecked")
	public void addRawBackgroundHistogram(TagCountHistogram tags)
	{
		Series<Double, Double> serie = Histogram.getHistogram(tags, "Background histogram");
		histogram.getData().add(serie);


	}



	@SuppressWarnings("unchecked")
	public void addPvalueData(Replicate rep, DataMapper mapper)
	{
	biovis.sierra.client.GUI.GUIHelper.BarChart.getPValueHist(rep , "P-value distribution", mapper, pvaluehist);
		
//		pvaluehist.getData().add(serie);
//		pvaluehist.setAnimated(false);
//		 serie.nodeProperty().get().setStyle("-fx-stroke: sandybrown;");
	
//		 serie.nodeProperty().get().setStyle("-fx-background-color: sandybrown,white;");
		
	
	}
	@SuppressWarnings("unchecked")
	public void addPvalueMedian(List<Double>px ,int median)
	{
		//		System.err.println(median);
//		Series<Double, Integer> serie = Histogram.getHistogram(px, median, "P-value median = "+median);
//		
//		pvaluehist.getData().add(serie);
////		 serie.nodeProperty().get().setStyle("-fx-stroke: #D2691E;");
//		 
//		 
//		 String seriesClass = null;
//		 for(String styleClass : serie.getNode().getStyleClass())
//		 {
//		     if(styleClass.startsWith("series"))
//		     {
//		         seriesClass = styleClass;
//		         System.err.println(seriesClass);
//		         break;
//		     }
//		 }

	}
	
	@SuppressWarnings("unchecked")
	public void addsSigHistData(Replicate rep, DataMapper mapper, String name)
	{
//		Series<Integer, Integer> serie = Histogram.getIntegerHistogram(px, py, "significant windows for replicate: "+ px.get(value));
		this.rep = rep;
		this.mapper = mapper;
		Series<String, Integer> serie = biovis.sierra.client.GUI.GUIHelper.BarChart.getSigHist(rep, name);
		sigHist.getData().add(serie);
		serie = biovis.sierra.client.GUI.GUIHelper.BarChart.getBarChart( mapper, "Median");
		sigHist.getData().add(serie);
		sigHist.setAnimated(false);
		
	}


	public void createBoxPlot(String replicate, Replicate rep)
	{
		final BoxPlot demo = new BoxPlot("Tag quality", replicate, rep);
		demo.setVisible(true);
		demo.setBackground(new Color(244,244,244));
		ChartCanvas canvas = new ChartCanvas(demo.chart);
		//		canvas.removeChartMouseListener(listener);
		StackPane stackPane = new StackPane(); 
		stackPane.setMinSize(0, 0);
		stackPane.setPrefSize(500, 500);
		stackPane.autosize();
		stackPane.getChildren().add(canvas); 
		gPane.add(stackPane, 2, 0);
//		canvas.setWidth(500);
//		canvas.setHeight(500);
		canvas.widthProperty().bind( stackPane.widthProperty()); 
		canvas.heightProperty().bind(stackPane.heightProperty()); 
		stackPane.requestLayout();
		
		stackPane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
//					System.out.println("consuming right release button in cm filter");
//					WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
//					exportImage(snapshot);
					SingleFigure figure = new SingleFigure();
					figure.createBoxPlot("Tag quality", replicate, rep);
//					System.err.println(chart.getData().);
//					System.err.println(x.hashCode());

					event.consume();
				}
				
			}
				}
				);
		
		
		
	}
//	@SuppressWarnings("unchecked")
//	public void addSignificantWindows(ArrayList<Integer>px , ArrayList<Integer> py, int value)
//	{
//		//		System.err.println(median);
//		Series<Integer, Integer> serie = Histogram.getIntegerHistogram(px, py, "significant windows for replicate: "+ px.get(value));
//		sigHist.getData().add(serie);
//		sigHist.setAnimated(false);
//	}
//	@SuppressWarnings("unchecked")
//	public void addSignificantWindows(ArrayList<Integer>px , Integer median)
//	{
//		Series<Integer, Integer> serie = Histogram.getIntegerHistogram(px, median, "median = "+median);
//		sigHist.getData().add(serie);
//	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void initTab(double least, int max, int maxPValue)
	{
		final NumberAxis gxAxis = new NumberAxis();
		final NumberAxis gyAxis = new NumberAxis();
		
		final NumberAxis lxAxis = new NumberAxis();
		final NumberAxis lyAxis = new NumberAxis();
		//Magic Number 0.4: Value 0.0 on a logaritmic scale is not useful, distance 0.4 to 1.0 and 1.0 to 3.164 is quite similar.
		final Axis qxAxis = new LogarithmicAxis(0.4,max);
		final NumberAxis qyAxis = new NumberAxis();
		
		final CategoryAxis pxAxis = new CategoryAxis();
		final LogarithmicAxis pyAxis = new LogarithmicAxis(0.4,maxPValue+10000);
		
		final CategoryAxis kxAxis = new CategoryAxis();
		final NumberAxis kyAxis = new NumberAxis();
		norm = new LineChart(gxAxis, gyAxis);
		
		raw = new LineChart(lxAxis, lyAxis);
		histogram = new LineChart(qxAxis, qyAxis);
		pvaluehist = new BarChart(pxAxis, pyAxis);
		sigHist = new BarChart(kxAxis, kyAxis);
		sigHist.setCategoryGap(2);
		sigHist.setBarGap(0);
		norm = addListener(norm, gxAxis, true, false, false, -1);
		raw = addListener(raw, lxAxis, true, false,false, -1);
		histogram = addListener(histogram, qxAxis, false, true ,false, max);
		
		pvaluehist = addListener(pvaluehist, "pValueHist", maxPValue+10000);
		sigHist = addListener(sigHist, "sigHist", 0);
		raw.getStylesheets().clear();
		raw.getStylesheets().add(getClass().getResource("linechart.css").toExternalForm());
		pvaluehist.getStylesheets().clear();
		pvaluehist.getStylesheets().add(getClass().getResource("histogram.css").toExternalForm());
		pvaluehist.setCategoryGap(2);
		pvaluehist.setBarGap(0);
//		String result = String.format("%.6f", least);
		
		norm.setTitle("Normalized estimated poisson distribution");
		raw.setTitle("Estimated poisson distribution");
		histogram.setTitle("Count distribution for experiment and background");
		pvaluehist.setTitle("P-value distribution over all windows");
		sigHist.setTitle("Distribution of significant windows");
//		System.err.println(pvaluehist.getStylesheets().toString());
		gPane = new GridPane();

		//Set constraint for the column size
		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(33);
		gPane.getColumnConstraints().add(column1);
		gPane.getColumnConstraints().add(column1);
		gPane.getColumnConstraints().add(column1);
		//Set constraint for the row sizes
		RowConstraints row1 = new RowConstraints();
		row1.setPercentHeight(50);
		gPane.getRowConstraints().add(row1);
		gPane.getRowConstraints().add(row1);

		gPane.add(norm, 0, 1);
		gPane.add(raw, 0, 0);
		gPane.add(histogram, 1, 0);
		gPane.add(pvaluehist, 1, 1);
		gPane.add(sigHist, 2, 1);
		gPane.setGridLinesVisible(true);
		gPane.autosize();
		

		tab.setContent(gPane);

	}

//
//	private BarChart addPValueListener(BarChart pvaluehist) {
//		pvaluehist.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
//				{
//			@Override
//			public void handle(MouseEvent event) {
//				if (event.getButton() == MouseButton.SECONDARY) {
////					System.out.println("consuming right release button in cm filter");
////					WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
////					exportImage(snapshot);
//
//					Series<Object, Object> temp = (Series<Object, Object>) pvaluehist.getData().get(0);
//					SingleFigure figure = new SingleFigure();
//					figure.createSingleBarChart(rep, mapper, pvaluehist.getTitle(), temp.getName());
////					System.err.println(chart.getData().);
////					System.err.println(x.hashCode());
//
//					event.consume();
//				}
//				
//			}
//				}
//				);
//		return pvaluehist;
//		
//	}



	public WritableImage exportImage()
	{
		//		System.err.println("-2");
		WritableImage snapshot = gPane.snapshot(new SnapshotParameters(), null);
		//		 System.err.println("-1");
		return snapshot;
	}
	public int getGpaneHeight()
	{
		return (int) gPane.getHeight();
	}
	public int getGpaneWidth()
	{
		return (int)gPane.getWidth();
	}

	
	@SuppressWarnings("rawtypes")
	private BarChart addListener(BarChart bchart, String type, int max)
	{
		bchart.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
//					System.out.println("consuming right release button in cm filter");
//					WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
//					exportImage(snapshot);

					Series<Object, Object> temp = (Series<Object, Object>) bchart.getData().get(0);
					SingleFigure figure = new SingleFigure();
					figure.createSingleBarChart(rep, mapper, bchart.getTitle(), temp.getName(), type, max);
//					System.err.println(chart.getData().);
//					System.err.println(x.hashCode());

					event.consume();
				}
				
			}
				}
				);
		return bchart;
	}
	

	@SuppressWarnings("rawtypes")
	private LineChart addListener(LineChart chart, Axis x, boolean range, boolean hist, boolean pvalue,int max)
	{
		
		
		
		chart.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
//					System.out.println("consuming right release button in cm filter");
//					WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
//					exportImage(snapshot);
					SingleFigure figure = new SingleFigure();
					figure.createSingleFigure(chart.getData(),range, chart.getTitle(), hist, pvalue,max);
//					System.err.println(chart.getData().);
//					System.err.println(x.hashCode());

					event.consume();
				}
				
			}
				}
				);
		
		
		if(range)
		{
		final double SCALE_DELTA = 1.1;
		
		
		
		
		chart.setOnScroll(new EventHandler<ScrollEvent>() {
                    @Override
		    public void handle(ScrollEvent event) {
		        event.consume();
//		        System.err.println("bla");
		        if (event.getDeltaY() == 0) {
		            return;
		        }
		        x.setAutoRanging(false);
		        double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
		        ((NumberAxis)x).setUpperBound(((NumberAxis) x).getUpperBound()*scaleFactor);
		       
		    }
		});

		chart.setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override
		    public void handle(MouseEvent event) {
		        if (event.getClickCount() == 2) {

		        	 x.setAutoRanging(true);
		        }
		    }
		});
		}
		return chart;
		
	}

}
