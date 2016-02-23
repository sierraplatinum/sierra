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

import biovis.sierra.client.Commander.LocalServer;
import biovis.sierra.client.Commander.PeakCommander;
import biovis.sierra.client.GUI.GUIHelper.MD5Generator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Modality;
import javafx.stage.Stage;
/**
*
* @author Daniel Gerighausen
*/
public class ServerDialog {

	private TextField hostField;
	private TextField portField;
	private FXMLDocumentController fxmlcontroll;
	private Stage myDialog;
	private TextField passField;
	private TextField clientportField;
	private CheckBox cLocalServer;


	Preferences prefs = Preferences.userNodeForPackage(ServerDialog.class);


	public void createServerDialog(FXMLDocumentController  fxmlcontroll)
	{
		this.fxmlcontroll = fxmlcontroll;
		myDialog = new Stage();
		myDialog.initModality(Modality.WINDOW_MODAL);

		GridPane serverPane = new GridPane();
		serverPane.setPrefSize(500, 300);

		ColumnConstraints cConstraint = new ColumnConstraints();
		cConstraint.setPercentWidth(33.33);
		RowConstraints rConstraint = new RowConstraints();
		rConstraint.setPercentHeight(16.66);

		serverPane.getColumnConstraints().add(cConstraint);
		serverPane.getRowConstraints().add(rConstraint);
		serverPane.getRowConstraints().add(rConstraint);
		serverPane.getRowConstraints().add(rConstraint);
		serverPane.getRowConstraints().add(rConstraint);
		serverPane.getRowConstraints().add(rConstraint);
		serverPane.getRowConstraints().add(rConstraint);
		Label host = new Label("Host: ");
		Label port = new Label("Server port: ");
		Label clientPort = new Label("Client port: ");
		Label password = new Label("Password: ");
		Label localServer = new Label("Start local server");
		cLocalServer = new CheckBox();
		cLocalServer.setSelected(prefs.getBoolean("localServer", true));

		hostField = new TextField(prefs.get("server", "localhost"));
		portField = new TextField(prefs.get("SPort", "9753"));
		clientportField = new TextField(prefs.get("CPort", "9754"));
		passField = new TextField(prefs.get("Password", "Swordfish"));
		hostField.setEditable(true);
		portField.setEditable(true);
		clientportField.setEditable(true);
		passField.setEditable(true);


		serverPane.add(host, 0, 0);
		serverPane.add(port, 0, 1);
		serverPane.add(clientPort, 0, 2);
		serverPane.add(password, 0, 3);
		serverPane.add(localServer, 0,4);


		serverPane.add(hostField, 1, 0);
		serverPane.add(portField, 1, 1);
		serverPane.add(clientportField, 1, 2);
		serverPane.add(passField, 1, 3);
		serverPane.add(cLocalServer, 1, 4);


		Button connect = new Button("Connect");
		connect.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent e) {
//				System.err.println("Klick");
				connectServer();
			}
		});
		serverPane.add(connect, 1, 5);

		Scene myDialogScene = new Scene(serverPane);


		myDialog.setScene(myDialogScene);
		myDialog.setTitle("Connection Manager");
		myDialog.show();
	}

	private void connectServer()
	{


		if(cLocalServer.isSelected())
		{
			if(!LocalServer.isRunning())
			{
				LocalServer.startLocalServer();
			}
		}
		if(LocalServer.isRunning() || !cLocalServer.isSelected())
		{
			PeakCommander pc = fxmlcontroll.getPeakCommander();
			pc.stopListener();
			pc = new PeakCommander(fxmlcontroll);
			if(pc.setServer(hostField.getText(), Integer.valueOf(portField.getText()),  Integer.valueOf(clientportField.getText())))
			{
				fxmlcontroll.setPeakCommander(pc);
				


				Object[] command = new Object[2];
				command[0] = "credentials";
				Object[] payload = new Object[2];
				String password = new MD5Generator().getMD5(passField.getText());
				payload[0] = password;
				payload[1] = pc.getHash();
				command[1] = payload;
				pc.sendCommand(command);
				System.out.println("Pulling mapper");
				fxmlcontroll.pullData();
			
				command = new Object[2];
				command[0] = "getProgress";
				command[1] = "getProgress";
				pc.sendCommand(command);
				pc.ack();

				prefs.put("server", hostField.getText());

				prefs.put("SPort", portField.getText());

				prefs.put("CPort", clientportField.getText());

				prefs.put("Password", passField.getText());

				prefs.putBoolean("localServer", cLocalServer.isSelected());

				myDialog.close();
			}
			else
			{
				new ErrorDialog().init("Connection to server failed!");
			}
		}
		else
		{
			new ErrorDialog().init("Local server is already running!");
		}

	}




}