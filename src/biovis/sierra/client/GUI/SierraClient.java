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


import java.util.prefs.Preferences;

import biovis.sierra.client.Commander.PeakCommander;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
/**
*
* @author Daniel Gerighausen
*/
public class SierraClient extends Application {

	public int clientSocket = 9754;
	Preferences prefs = Preferences.userNodeForPackage(SierraClient.class);

	@Override
	public void start(Stage stage) throws Exception {
	 
		FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));
		Parent root = (Parent)loader.load();
		FXMLDocumentController controller = (FXMLDocumentController)loader.getController();
		PeakCommander pc = new PeakCommander(controller);
		controller.setPeakCommander(pc);
	
		Scene scene = new Scene(root);
		
		//        File progress = new File("progress.css");
		
		//        scene.getStylesheets().add("file:///" + progress.getAbsolutePath().replace("\\", "/"));
		stage.setScene(scene);
		stage.show();
		stage.setTitle("Sierra Platinum");
		//        Image test = new Imag
		//        System.err.println(picture.getFile());
		Image ico = new Image(getClass().getResource("sierra.png").toExternalForm());

		stage.setMaximized(true);
		// Get current screen of the stage      
		ObservableList<Screen> screens = Screen.getScreensForRectangle(new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));

		// Change stage properties
		Rectangle2D bounds = screens.get(0).getVisualBounds();
		stage.setX(bounds.getMinX());
		stage.setY(bounds.getMinY());
		stage.setWidth(bounds.getWidth());
		stage.setHeight(bounds.getHeight());
	
		stage.getIcons().add(ico);

	


		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent t) {

				Platform.runLater(new Runnable() {

					@Override
					public void run() {

//						System.err.println(pc.isActive());
						if(pc.isActive())
						{
						Object[] c = {"QUIT","QUIT"};
						pc.sendCommand(c);
						}
						System.out.println("Application Closed by click to Close Button(X)");
						System.exit(0);
					}
				});

			}

		});
		
		
		
		ServerDialog reconnect= new ServerDialog();
		reconnect.createServerDialog(controller);

	}
	public static void main(String[] args) {

	
		launch(args);
		
		
		
	}






}
