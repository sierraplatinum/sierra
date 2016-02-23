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

import java.awt.geom.Rectangle2D;

import javafx.scene.canvas.Canvas;

import org.jfree.chart.JFreeChart;
import org.jfree.fx.FXGraphics2D;
/**
*
* @author Daniel Gerighausen
*/
public class  ChartCanvas extends Canvas { 

	JFreeChart chart;

	private FXGraphics2D g2;

	public ChartCanvas(JFreeChart chart) {
		this.chart = chart;
		this.g2 = new FXGraphics2D(getGraphicsContext2D());
		widthProperty().addListener(evt -> draw()); 
		heightProperty().addListener(evt -> draw()); 
	}  

	private void draw() { 
		double width = getWidth(); 
		double height = getHeight();
//		System.err.println("redraw Height: "+height);
//		System.err.println("redraw Width: "+width);
		getGraphicsContext2D().clearRect(0, 0, width, height);
		this.chart.draw(this.g2, new Rectangle2D.Double(0, 0, width, 
				height));
	
	} 

	@Override 
	public boolean isResizable() { 
		return true;
	}  

	@Override 
	public double prefWidth(double height) { return getWidth(); }  

	@Override 
	public double prefHeight(double width) { return getHeight(); } 
} 


