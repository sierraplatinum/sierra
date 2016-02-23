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

import org.apache.commons.math3.distribution.PoissonDistribution;

import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
/**
*
* @author Daniel Gerighausen
*/
public class Poisson {

	PoissonDistribution ptest;
		

	public  Series<Integer, Double> getPoisson(double lambda, String name, double range)
	{
		
		//Testing
		createTestPoisson(lambda);
		
		
		Series<Integer, Double> series = new XYChart.Series<>();
		series.setName(name);
		
		for(int i = 0; i <= range; i++)
		{
				
						
			
			series.getData().add(new Data<Integer, Double>(i,ptest.probability(i)));
			
			
		}
		
	
		




		return series;


	}
	public void setPoisson(PoissonDistribution poisson)
	{
		ptest = poisson;
	}

	
	
		private void createTestPoisson(double lambda)
		{
			ptest = new PoissonDistribution(lambda);
		}
}
