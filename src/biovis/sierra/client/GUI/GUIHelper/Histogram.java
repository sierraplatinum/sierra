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
package biovis.sierra.client.GUI.GUIHelper;

import java.util.ArrayList;

import biovis.sierra.data.peakcaller.TagCountHistogram;
import java.util.List;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
/**
*
* @author Daniel Gerighausen, Dirk Zeckzer
*/
public class Histogram {

	public static Series<Double, Double> getHistogram(TagCountHistogram tags, String name)
	{
		Series<Double, Double> series = new XYChart.Series<>();
		series.setName(name);
		//Magic Number 0.4: Value 0.0 on a logaritmic scale is not useful, distance 0.4 to 1.0 and 1.0 to 3.164 is quite similar.
		series.getData().add(new Data<>(0.4,tags.getBins().get(0)));
		
		for(int i = 1; i < tags.getBins().size(); i++)
		{
			series.getData().add(new Data<>((double)tags.getBinXValue().get(i),tags.getBins().get(i)));
		}

		return series;
	}

	public static Series<Double, Integer> getDoubleHistogram(List<Double> px, List<Integer> py, String name)
	{
		Series<Double, Integer> series = new XYChart.Series<>();
		series.setName(name);
		series.getData().add(new Data<>(0.0,py.get(0)));
		for(int i = 0; i < px.size()-1; i++)
		{
			if(i > 0)
			{
				series.getData().add(new Data<>(px.get(i-1),py.get(i)));
			}
//			System.err.println(px.get(i) + " " +py.get(i));
			series.getData().add(new Data<>(px.get(i),py.get(i)));
		}
		series.getData().add(new Data<>(px.get(px.size()-2),py.get(px.size()-1)));
		series.getData().add(new Data<>(1.0,py.get(px.size()-1)));
		return series;
	}

	public static Series<Double, Integer> getHistogram(List<Double> px,Integer median, String name)
	{
		Series<Double, Integer> series = new XYChart.Series<>();
		series.setName(name);
		series.getData().add(new Data<>(0.0,median));
		for(int i = 0; i < px.size()-1; i++)
		{
			if(i > 0)
			{
				series.getData().add(new Data<>(px.get(i-1),median));
			}
//			System.err.println(px.get(i) + " " +py.get(i));
			series.getData().add(new Data<>(px.get(i),median));
		}
		series.getData().add(new Data<>(px.get(px.size()-2),median));
		series.getData().add(new Data<>(1.0,median));

		return series;
	}

	public static Series<Integer, Integer> getIntegerHistogram(ArrayList<Integer> px, ArrayList<Integer> py, String name)
	{
		Series<Integer, Integer> series = new XYChart.Series<>();
		series.setName(name);

		for(int i = 0; i < px.size(); i++)
		{
			series.getData().add(new Data<>(px.get(i),py.get(i)));
		}

		return series;
	}

	public static Series<Integer, Integer> getIntegerHistogram(ArrayList<Integer> px,Integer median, String name)
	{
		Series<Integer, Integer> series = new XYChart.Series<>();
		series.setName(name);

		for(int i = 0; i <= px.size(); i++)
		{	
			series.getData().add(new Data<>(median,i));
		}

		return series;
	}	
}
