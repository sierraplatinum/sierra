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

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
/**
*
* @author Daniel Gerighausen
*/
public class ErrorDialog {
	public void init(String error)
	{
		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				FXMLLoader loader = new FXMLLoader();
				try {

					Stage myDialog = new Stage();
					myDialog.initModality(Modality.WINDOW_MODAL);

					Parent root = (Parent) loader.load(getClass().getResourceAsStream("ErrorDialog.fxml"));
					ErrorDialogController controller = loader.getController();
					controller.setErrorMessage(error);
					Scene scene = new Scene(root);
					myDialog.setScene(scene);
					myDialog.show();
					//			tab.setContent(root);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}});
	}
}
