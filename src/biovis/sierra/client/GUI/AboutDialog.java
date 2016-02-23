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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
/**
*
* @author Daniel Gerighausen
*/
public class AboutDialog {

	public AboutDialog()
	{

	}

	public void createAboutDialog()
	{
		final Stage myDialog = new Stage();
		myDialog.initModality(Modality.WINDOW_MODAL);


		VBox vbox = new VBox();


		vbox.setAlignment(Pos.CENTER);

		URL picture = getClass().getResource("sierra.png");
		System.err.println(picture.getFile());
		Image ico = new Image(getClass().getResource("sierra.png").toExternalForm());
		ImageView imgView = new ImageView(ico);
		vbox.getChildren().add(imgView);

		vbox.setPadding(new Insets(10));
		vbox.getChildren().add(new Text("Sierra Platinum"));
		vbox.getChildren().add(new Text("Version 0.1"));
		vbox.getChildren().add(new Text("Daniel Gerighausen, Lydia Müller, and Dirk Zeckzer"));
		vbox.getChildren().add(new Text("BSV & BioInf Leipzig University"));
		//      vbox.getChildren


		vbox.setPadding(new Insets(25));
		picture = getClass().getResource("prussian.png");
		System.err.println(picture.getFile());
		ico = new Image(getClass().getResource("prussian.png").toExternalForm());

	       System.err.println(getClass().getResource("prussian.png").toExternalForm());
		imgView = new ImageView(ico);
		vbox.getChildren().add(imgView);

		
		vbox.setPadding(new Insets(10));


		Scene myDialogScene = new Scene(vbox);


		myDialog.setScene(myDialogScene);
		myDialog.setTitle("About");
		myDialog.show();
	}

}
