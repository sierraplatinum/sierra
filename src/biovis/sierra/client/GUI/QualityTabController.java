package biovis.sierra.client.GUI;

import java.awt.Color;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

import biovis.sierra.client.GUI.GUIHelper.BoxPlot;
import biovis.sierra.client.GUI.GUIHelper.ChartCanvas;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.peakcaller.IntBoxplotData;
import biovis.sierra.data.peakcaller.PeakQuality;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
/**
*
* @author Daniel Gerighausen
*/
public class QualityTabController implements Initializable  {

	
	@FXML
	private GridPane qualPane;
	
	int column = 0;
	int row = 0;
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		
	}
	
	public void initPlots(DataMapper mapper)
	{
		
		PeakQuality broad = mapper.getBroadPeakQuality();
		
//		System.err.println(broad.getNumberOfReplicates());
		
		Set<Integer> replicateSet = broad.getReplicatesWithQuality();
		
		for(Integer number : replicateSet)
		{
//			System.err.println(number);
//			System.err.println(broad.getBackgroundFor(number).toString());
//			System.err.println(broad.getExperimentFor(number).toString());
			
			qualPane.add(createBoxPlot(mapper.getReplicates().get(number).getName(), broad.getExperimentFor(number), broad.getBackgroundFor(number)), column, row);
			
			qualPane.setAlignment(Pos.CENTER);
			qualPane.autosize();
			
			column = (column+1) %2;
			if(column == 0)
			{
				row++;
			}
			
		}
		
		
		
		
		
	}
	
	
	private StackPane createBoxPlot(String replicate, IntBoxplotData experimentData,IntBoxplotData backgroundData )
	{
		System.err.println(replicate);
		final BoxPlot demo = new BoxPlot("Tag quality", replicate, experimentData, backgroundData);
		demo.setVisible(true);
		demo.setBackground(new Color(244,244,244));
		ChartCanvas canvas = new ChartCanvas(demo.chart);
		//		canvas.removeChartMouseListener(listener);
		StackPane stackPane = new StackPane(); 
		stackPane.setMinSize(0, 0);
		stackPane.setPrefSize(500, 500);
		stackPane.setMaxWidth(500);
//		stackPane.autosize();
		stackPane.getChildren().add(canvas); 
//		qualityPane.add(stackPane, 2, 0);
//		canvas.setWidth(500);
//		canvas.setHeight(500);
		canvas.widthProperty().bind( stackPane.widthProperty()); 
		canvas.heightProperty().bind(stackPane.heightProperty()); 
		stackPane.requestLayout();
		stackPane.setAlignment(Pos.CENTER);
		
		stackPane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
				{
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
//					System.out.println("consuming right release button in cm filter");
//					WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
//					exportImage(snapshot);
					SingleFigure figure = new SingleFigure();
					figure.createBoxPlot("Tag quality", replicate, experimentData, backgroundData);
//					System.err.println(chart.getData().);
//					System.err.println(x.hashCode());

					event.consume();
				}
				
			}
				}
				);
		
		return stackPane;
		
	}
	
	public WritableImage exportImage()
	{
		//		System.err.println("-2");
		WritableImage snapshot = qualPane.snapshot(new SnapshotParameters(), null);
		//		 System.err.println("-1");
		return snapshot;
	}
	public int getHeight()
	{
		return (int) qualPane.getHeight();
	}
	public int getWidth()
	{
		return (int) qualPane.getWidth();
	}

}
