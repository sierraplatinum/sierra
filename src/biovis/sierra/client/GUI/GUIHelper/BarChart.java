package biovis.sierra.client.GUI.GUIHelper;

import java.util.List;
import java.util.Map.Entry;

import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
/**
 *
 * @author Daniel Gerighausen
 */
public class BarChart {

	public static Series<String, Integer> getSigHist(Replicate rep,  String name)
	{
		Series<String, Integer> series = new XYChart.Series<>();
		series.setName(name);
		//Magic Number 0.4: Value 0.0 on a logaritmic scale is not useful, distance 0.4 to 1.0 and 1.0 to 3.164 is quite similar.
		//		series.getData().add(new Data<>(0.4,tags.getBins().get(0)));


		for( Entry<String, Integer> entry : rep.getSignificantWindowsChrWise().entrySet())
		{
			//			System.err.println(entry.getKey() + ": "+entry.getValue());
			if(!entry.getKey().contains("_"))
			{


				if(entry.getValue() > 0)
				{

					series.getData().add(new Data<>(entry.getKey(), entry.getValue()));
				}
			}
		}




		return series;
	}
	public static void getPValueHist(Replicate rep,  String name, DataMapper mapper, @SuppressWarnings("rawtypes") javafx.scene.chart.BarChart pvaluehist)
	{


		Series<String, Integer> series = new XYChart.Series<>();
//		series.setName(name);
		//Magic Number 0.4: Value 0.0 on a logaritmic scale is not useful, distance 0.4 to 1.0 and 1.0 to 3.164 is quite similar.
		//		series.getData().add(new Data<>(0.4,tags.getBins().get(0)));

		
			for(int i = 0; i < rep.getFinalPValueExp().size(); i++)
			{
				series.getData().add(
						new XYChart.Data<>(
								String.valueOf(rep.getFinalPValueExp().get(i).getFirst()),
								rep.getFinalPValueExp().get(i).getSecond()));
		
			}
			series.setName(rep.getName());
			pvaluehist.getData().add(series);
			
			
			
			Series<String, Integer> median = new XYChart.Series<>();
			median.setName("Median");
			
			
			//Magic Number 0.4: Value 0.0 on a logaritmic scale is not useful, distance 0.4 to 1.0 and 1.0 to 3.164 is quite similar.
			//		series.getData().add(new Data<>(0.4,tags.getBins().get(0)));

				
				for(int i = 0; i < mapper.getPValueMedianExponents().size(); i++)
				{
//					System.err.println(i);
					median.getData().add(
							new XYChart.Data<>(
									String.valueOf(mapper.getPValueMedianExponents().get(i).getFirst()),
									mapper.getPValueMedianExponents().get(i).getSecond()));
			
				}
//				series.setName(String.valueOf(rep.getIndex()));
				pvaluehist.getData().add(median);
//		return series;
	}

	public static Series<String, Integer> getBarChart(DataMapper mapper,  String name)
	{
		Series<String, Integer> series = new XYChart.Series<>();
		series.setName(name);
		//Magic Number 0.4: Value 0.0 on a logaritmic scale is not useful, distance 0.4 to 1.0 and 1.0 to 3.164 is quite similar.
		//		series.getData().add(new Data<>(0.4,tags.getBins().get(0)));


		for( Entry<String, Integer> entry : mapper.getSignifcantWindowMedianChrWise().entrySet())
		{
			//			System.err.println(entry.getKey() + ": "+entry.getValue());
			if(!entry.getKey().contains("_"))
			{
				if(entry.getValue() > 0)
				{
					series.getData().add(new Data<>(entry.getKey(), entry.getValue()));
				}
			}
		}




		return series;
	}
}
