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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biovis.sierra.client.GUI;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.vfs2.FileObject;

import com.google.gson.Gson;
import biovislib.vfsjfilechooser.VFSJFileChooser;
import biovislib.vfsjfilechooser.VFSJFileChooser.RETURN_TYPE;
import biovislib.vfsjfilechooser.VFSJFileChooser.SELECTION_MODE;
import biovislib.vfsjfilechooser.accessories.DefaultAccessoriesPanel;
import biovislib.vfsjfilechooser.filechooser.VFSFileFilter;

import biovis.sierra.client.Commander.PeakCommander;
import biovis.sierra.client.GUI.GUIHelper.RandomDNA;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.IO.Exporter;
import biovis.sierra.data.IO.Importer;
import biovis.sierra.data.peakcaller.PeakList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Arc;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 *
 * @author Daniel Gerighausen, Dirk Zeckzer
 */
@SuppressWarnings("deprecation")
public class FXMLDocumentController implements Initializable {

	@FXML
	private Button experiment;
	@FXML
	private Button start;
	@FXML
	private TreeView<String> treeView;
	@FXML
	private TabPane tabPane;
	@FXML
	private TextField jobName;
	@FXML
	private TextField pcutoff;
	@FXML
	private TextField offset;
	@FXML
	private ProgressBar progress;
	@FXML
	private TextField windowsize;
	@FXML
	private MenuItem mClose;
	@FXML
	private MenuItem mCOpen;
	@FXML
	private MenuItem mSOpen;
	@FXML
	private MenuItem mCSave;
	@FXML
	private MenuItem mSSave;
	@FXML
	private MenuItem mPSave;
	@FXML
	private MenuItem mDLoad;
	@FXML
	private MenuItem mDSave;
	@FXML
	private GridPane controllpane;
	@FXML
	private Arc pacMan;
	@FXML
	private Label dnaLabel;
	@FXML
	private MenuItem mKillBill;
	@FXML
	private MenuItem mPullBroad;
	@FXML
	private MenuItem mPullNarrow;
	@FXML
	private MenuItem mSaveState;
	@FXML
	private HBox spinnerPane;

	//Do not use this, the fxml implementation of this is still buggy (no fixed min value)
	//	@FXML
	//	private Spinner frameSpinner;

	/** End FXML variables *******************************************************/

	boolean choosing = false;
	int counter = 1;
	private DataMapper mapper;
	private int datasetsReady = 0;

	private List<ReplicateTab> tabviews;
	private PeakList pList;
	private String pDescription;
	private ServerDialog server ;
	private PeakCommander peakCommander;
	private final double distLength = 60;
	private final double distAngle = 30;
	private  double lengthValue = distLength;
	private  double startAngle = 0;
	private Spinner<Integer> adjustCores;
	private RandomDNA dna;

	// Remote file chooser
	private VFSJFileChooser remoteFileChooser;
	private biovislib.vfsjfilechooser.accessories.DefaultAccessoriesPanel remoteFileChooserConf;

	private Preferences prefs;
	private ResultTab results;
	private peakTabController pTC;
	private QualityTabController qualController;
	private int tab = 0;
	private ReceiverDialog receiver;


	@Override
	public void initialize(URL url, ResourceBundle rb) {
		prefs = Preferences.userNodeForPackage(FXMLDocumentController.class);
		init();

		remoteFileChooser = new VFSJFileChooser(); // create a file dialog

		remoteFileChooserConf = new DefaultAccessoriesPanel(remoteFileChooser);
		remoteFileChooserConf.setHost(prefs.get("hostname", ""));
		remoteFileChooserConf.setPort(prefs.get("port", "22"));
		remoteFileChooserConf.setUser(prefs.get("user", ""));
		remoteFileChooserConf.setPath(prefs.get("path", ""));

		// configure the file dialog
		remoteFileChooser.setAccessory(remoteFileChooserConf);
		remoteFileChooser.setFileHidingEnabled(false);
		remoteFileChooser.setMultiSelectionEnabled(false);
		remoteFileChooser.setFileSelectionMode(SELECTION_MODE.FILES_ONLY);

		mClose.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
		mCOpen.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));
		//		mSOpen.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
		mDLoad.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN));

		mCSave.setAccelerator(new KeyCodeCombination(KeyCode.J, KeyCombination.CONTROL_DOWN));
		//		mSSave.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN));
		mPSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		mDSave.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
		//		 tabPane.getSelectionModel()
		//         .selectedItemProperty()
		//         .addListener(
		//                 (obs, oldTab, newTab) -> {
		//                	 System.err.println(obs);
		//                	 System.err.println(oldTab);
		//                	 System.err.println(newTab);
		//                 });
		//		 tabPane.

		adjustCores = new Spinner<Integer>(1, 1000, 4);
		adjustCores.setEditable(true);
		adjustCores.valueProperty().addListener(
				(obs, oldValue, newValue) -> mapper.setNumCores(newValue));



		adjustCores.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
			try {
				if (isInteger(newValue)) {
					adjustCores.getValueFactory().setValue(Integer.parseInt(newValue));
				}
			} catch (NumberFormatException e) {
				if (isInteger(oldValue)) {
					adjustCores.getValueFactory().setValue(Integer.parseInt(oldValue));
				}
			}
		});
		spinnerPane.getChildren().add(adjustCores);
		//		controllpane.add(spinnerPane, 1, 2);
		dna = new RandomDNA();
		dnaLabel.setText(dna.generateStrand());

	}

	@FXML
	private void keyPressedPValue()
	{
		System.err.println(pcutoff.getText());
		//System.err.println(pcutoff.getText().matches("[a-z]*"));

		System.err.println(pcutoff.getText().matches("(0.[0-9]+)|([0-9]+[eE]-[1-9][0-9]*)"));
		start.setDisable(false);


	}

	@SuppressWarnings("unchecked")
	@FXML
	private void addReplicate(MouseEvent event) {
		TreeItem<String> rep = new TreeItem<>("Replicate " + counter);

		TreeItem<String> exp = new TreeItem<>("Experiment");

		TreeItem<String> background = new TreeItem<>("Background");





		rep.getChildren().addAll(exp, background);
		rep.setExpanded(true);

		treeView.getRoot().getChildren().add(rep);


		Replicate replicate = new Replicate("Replicate " + counter);
		mapper.addReplicate(replicate);

		ReplicateTab tview = new ReplicateTab("Replicate " + counter);
		tabviews.add(tview);
		//		Tab temp = tview.createTab("Replicate " + counter);

		//		tabPane.getTabs().add(temp);


		counter++;
		datasetsReady += 2;

	}

	@FXML
	private void startCalc(MouseEvent event)
	{

		if(treeView.getRoot().getChildren().size() > 0 && datasetsReady == 0)
		{
			progress.setProgress(-1);
			mapper.setJobName(jobName.getText());
			ObservableList<TreeItem<String>> treereps = treeView.getRoot().getChildren();
			for(int i = 0; i < treereps.size(); i++)
			{

				String experiment = treereps.get(i).getChildren().get(0).getChildren().get(0).getValue();
				String background = treereps.get(i).getChildren().get(1).getChildren().get(0).getValue();

				mapper.getReplicates().get(i).getBackground().setDescription(background);
				mapper.getReplicates().get(i).getExperiment().setDescription(experiment);
				mapper.getReplicates().get(i).setName(treereps.get(i).getValue());
				mapper.setOffset(Integer.parseInt(offset.getText()));
				mapper.setWindowsize(Integer.parseInt(windowsize.getText()));
				mapper.setPvaluecutoff(Double.parseDouble(pcutoff.getText()));

			}
			if(peakCommander.isActive())
			{
				Object[] command = new Object[2];
				command[0] = "setDataMapper";

				Gson gson = new Gson();
				String dataString = gson.toJson(mapper);
				command[1] = dataString;
				peakCommander.sendCommand(command);
				command = new Object[2];
				command[0] = "startCalc";
				command[1] = "bla";
				peakCommander.sendCommand(command);
				peakCommander.ack();
				mKillBill.setDisable(false);
				setBusy(true);
			}
		}
	}

	@FXML
	private void addDataset(MouseEvent event) {

		Integer index = treeView.getSelectionModel().selectedIndexProperty().getValue();

		if (index == -1) {
			return;
		}

		TreeItem<String> selected = treeView.getTreeItem(index);
		if (selected.getChildren().isEmpty()
				&& !choosing
				&& selected.getParent() != null) {
			if(event.getButton() == MouseButton.SECONDARY)
			{
				return;
			} 
			if (selected.getValue().startsWith("Experiment")
					&& selected.getChildren().isEmpty()
					&& !choosing) {
				choosing = true;

				remoteFileChooser.setDialogTitle("Change Experiment File");
//				File rPath = new File("/home/zeckzer/Forschung/Projekte/Leipzig/BioVis/PeakMe/testsmall");
//				remoteFileChooser.setCurrentDirectory(rPath);

				remoteFileChooser.addChoosableFileFilter(new VFSFileFilter("Bam file (*.bam)", "bam"));
				RETURN_TYPE answer = remoteFileChooser.showOpenDialog(null);

				// check if a file was selected
				if (answer == RETURN_TYPE.APPROVE)
				{
					// retrieve the selected file
					final FileObject aFileObject = remoteFileChooser.getSelectedFileObject();

					String path = aFileObject.getName().getPath();

					// display the file path
					if (path != null) {
						TreeItem<String> exp = new TreeItem<>(path);
						selected.getChildren().add(exp);
						selected.setExpanded(true);
						datasetsReady -=1;
					}

					prefs.put("hostname", remoteFileChooserConf.getHost());
					prefs.put("port", remoteFileChooserConf.getPort());
					prefs.put("user", remoteFileChooserConf.getUser());
					prefs.put("path", remoteFileChooserConf.getPath());


				}
				choosing = false;
			}
			if (selected.getValue().startsWith("Background")
					&& selected.getChildren().isEmpty()
					&& !choosing) {


				choosing = true;

				remoteFileChooser.setDialogTitle("Change Background File");
				File rPath = new File("/home/zeckzer/Forschung/Projekte/Leipzig/BioVis/PeakMe/testsmall");
				remoteFileChooser.setCurrentDirectory(rPath);

				remoteFileChooser.addChoosableFileFilter(new VFSFileFilter("Bam file (*.bam)", "bam"));
				RETURN_TYPE answer = remoteFileChooser.showOpenDialog(null);

				// check if a file was selected
				if (answer == RETURN_TYPE.APPROVE)
				{
					// retrieve the selected file
					final FileObject aFileObject = remoteFileChooser.getSelectedFileObject();
					String path = aFileObject.getName().getPath();

					// display the file path
					if (path != null) {
						TreeItem<String> exp = new TreeItem<>(path);
						selected.getChildren().add(exp);
						selected.setExpanded(true);
						datasetsReady -=1;
					}
				}
				choosing = false;

			}


			// Change existing entry
			File file;
			RETURN_TYPE answer;
			switch (selected.getParent().getValue()) {
			case "Experiment":
				choosing = true;

				remoteFileChooser.setDialogTitle("Change Experiment File");
				file =new File("/home/zeckzer/Forschung/Projekte/Leipzig/BioVis/PeakMe/testsmall");
				remoteFileChooser.setCurrentDirectory(file);

				remoteFileChooser.addChoosableFileFilter(new VFSFileFilter("Bam file (*.bam)", "bam"));
				answer = remoteFileChooser.showOpenDialog(null);

				// check if a file was selected
				if (answer == RETURN_TYPE.APPROVE)
				{
					// retrieve the selected file
					final FileObject aFileObject = remoteFileChooser.getSelectedFileObject();
					String path = aFileObject.getName().getPath();

					// display the file path
					if (path != null) {
						selected.setValue(path);
					}
				}
				choosing = false;
				break;
			case "Background":
				choosing = true;
				remoteFileChooser.setDialogTitle("Change Background File");
				file = new File("/home/zeckzer/Forschung/Projekte/Leipzig/BioVis/PeakMe/testsmall");
				remoteFileChooser.setCurrentDirectory(file);

				remoteFileChooser.addChoosableFileFilter(new VFSFileFilter("Bam file (*.bam)", "bam"));
				answer = remoteFileChooser.showOpenDialog(null);

				// check if a file was selected
				if (answer == RETURN_TYPE.APPROVE)
				{
					// retrieve the selected file
					final FileObject aFileObject = remoteFileChooser.getSelectedFileObject();
					String path = aFileObject.getName().getPath();

					// display the file path
					if (path != null) {
						selected.setValue(path);
					}
				}
				choosing = false;
				break;
			}
		}
	}

	@FXML
	private void close()
	{
		Stage stage = (Stage) start.getScene().getWindow();
		//		peakCommander.sendCommand("QUIT");
		if(peakCommander.isActive())
		{
			Object[] c = {"QUIT","QUIT"};
			peakCommander.sendCommand(c);
		}
		stage.close();
		System.exit(0);
	}


	@FXML
	private void saveConfig()
	{
		DataMapper temp = new DataMapper();
		temp.setJobName(jobName.getText());
		if(treeView.getRoot().getChildren().size() >= 1 && datasetsReady == 0)
		{
			ObservableList<TreeItem<String>> treereps = treeView.getRoot().getChildren();
			for(int i = 0; i < treereps.size(); i++)
			{
				Replicate rep = new Replicate(treereps.get(i).getValue());
				temp.addReplicate(rep);
				String experiment = treereps.get(i).getChildren().get(0).getChildren().get(0).getValue();
				String background = treereps.get(i).getChildren().get(1).getChildren().get(0).getValue();

				temp.getReplicates().get(i).getBackground().setDescription(background);
				temp.getReplicates().get(i).getExperiment().setDescription(experiment);
				temp.setOffset(Integer.parseInt(offset.getText()));
				temp.setWindowsize(Integer.parseInt(windowsize.getText()));
				temp.setPvaluecutoff(Double.parseDouble(pcutoff.getText()));
				temp.setNumCores(mapper.getNumCores());

			}

			FileChooser fileChooser = new FileChooser();
			File file;
			FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Sierra config files (*.csierra", "*.csierra");
			fileChooser.getExtensionFilters().add(extFilter);
			fileChooser.setTitle("Save config file");
			file = fileChooser.showSaveDialog(null);
			if (file != null) {

				String path = file.getAbsolutePath();

				if(!path.endsWith(".csierra"))
				{
					path +=".csierra";
				}

				System.err.println(path);

				Exporter.exportReplicates(path, temp.getReplicates(), temp);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@FXML
	private void loadConfig()
	{
		init();
		FileChooser fileChooser = new FileChooser();
		File file;
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Sierra config files (*.csierra", "*.csierra");
		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.setTitle("Open config file");
		file = fileChooser.showOpenDialog(null);
		if (file != null) {
			Importer.loadConfig(file.getAbsolutePath(), mapper);
			for(Replicate replicate : mapper.getReplicates())
			{
				TreeItem<String> rep = new TreeItem<>(replicate.getName());
				TreeItem<String> experiment = new TreeItem<>("Experiment");
				TreeItem<String> background = new TreeItem<>("Background");

				TreeItem<String> experiment_path = new TreeItem<>(replicate.getExperiment().getDescription());
				TreeItem<String> background_path = new TreeItem<>(replicate.getBackground().getDescription());

				experiment.getChildren().add(experiment_path);
				background.getChildren().add(background_path);
				experiment.setExpanded(true);
				background.setExpanded(true);

				rep.getChildren().addAll(experiment, background);
				rep.setExpanded(true);
				treeView.getRoot().getChildren().add(rep);

				ReplicateTab tview = new ReplicateTab(replicate.getName());
				tabviews.add(tview);

				counter++;

			}
			offset.setText(String.valueOf(mapper.getOffset()));
			pcutoff.setText(String.valueOf(mapper.getPvaluecutoff()));
			windowsize.setText(String.valueOf(mapper.getWindowsize()));
			adjustCores.getValueFactory().setValue(mapper.getNumCores());
			jobName.setText(mapper.getJobName());
		}
	}

	@FXML
	private void saveState()
	{
		FileChooser fileChooser = new FileChooser();

		//Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Sierra files (*.sierra", "*.sierra");
		fileChooser.getExtensionFilters().add(extFilter);
		File file;
		fileChooser.setTitle("Save state");
		file = fileChooser.showSaveDialog(null);
		if (file != null) {
			String path = file.getAbsolutePath();
			if(!path.endsWith(".sierra"))
			{
				path +=".sierra";
			}
			System.err.println(path);
		}
	}

	@FXML
	private void loadState() throws InterruptedException
	{
		init();
		FileChooser fileChooser = new FileChooser();
		File file;
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Sierra state files (*.sierra", "*.sierra");
		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.setTitle("Open sierra file");
		file = fileChooser.showOpenDialog(null);
		//		fileChooser.notify();
		fileChooser = null;

		if (file != null) {
			FXMLDocumentController temp = this;
			Task<Object> loader = new Task<Object>()
					{
				@Override
				protected Object call() throws Exception {

					Thread.sleep(500);
					Importer.loadState(file.getAbsolutePath(),temp);
					updateProgress(10, 10);

					Platform.runLater(new Runnable() {//updates ui on application thread
						@SuppressWarnings("unchecked")
						@Override
						public void run() {

							for(Replicate replicate : mapper.getReplicates())
							{
								TreeItem<String> rep = new TreeItem<>(replicate.getName());
								TreeItem<String> experiment = new TreeItem<>("Experiment");
								TreeItem<String> background = new TreeItem<>("Background");

								TreeItem<String> experiment_path = new TreeItem<>(replicate.getExperiment().getDescription());
								TreeItem<String> background_path = new TreeItem<>(replicate.getBackground().getDescription());

								experiment.getChildren().add(experiment_path);
								background.getChildren().add(background_path);
								experiment.setExpanded(true);
								background.setExpanded(true);

								rep.getChildren().addAll(experiment, background);
								rep.setExpanded(true);
								treeView.getRoot().getChildren().add(rep);

								ReplicateTab tview = new ReplicateTab("Replicate " + counter);
								tabviews.add(tview);

								counter++;

							}
							offset.setText(String.valueOf(mapper.getOffset()));
							pcutoff.setText(String.valueOf(mapper.getPvaluecutoff()));
							windowsize.setText(String.valueOf(mapper.getWindowsize()));

							drawPlots(mapper);

						}
					});

					return null;
				}

					};

					progress.progressProperty().bind(loader.progressProperty());
					Thread loaderThread = new Thread(loader);
					loaderThread.setDaemon(true);
					loaderThread.start();
		}
	}

	@FXML
	private void savePeaks()
	{
		FileChooser fileChooser = new FileChooser();

		//Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("bed files (*.bed", "*.bed");
		fileChooser.getExtensionFilters().add(extFilter);
		File file;
		fileChooser.setTitle("Save peaks as bed file");
		file = fileChooser.showSaveDialog(null);
		if (file != null) {
			String path = file.getAbsolutePath();
			if(!path.endsWith(".bed"))
			{
				path +=".bed";
			}
			Exporter.exportBED(path, pList, pDescription);
		}
	}

	@FXML
	private void openAbout()
	{
		new AboutDialog().createAboutDialog();
	}

	@FXML
	private void saveFigures()
	{
		FileChooser fileChooser = new FileChooser();

		//Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("png files (*.png", "*.png");
		fileChooser.getExtensionFilters().add(extFilter);
		File file;
		fileChooser.setTitle("Save figures as png files");
		file = fileChooser.showSaveDialog(null);

		if (file != null) {
			@SuppressWarnings("unused")
			int counter = 0;
			for(int i = 0; i < tabviews.size(); i++)
			{
				final int j = i;
				WritableImage image = tabviews.get(i).exportImage();
				Task<Void> imageexport = new Task<Void>()
						{
					@Override
					protected Void call() {

						String path = file.getAbsolutePath();
						if(!path.endsWith(".png"))
						{
							path = path + "-"+tabviews.get(j).getTab().getText() +".png";
						}
						else
						{
							path = path.replaceAll(".png", "-"+tabviews.get(j).getTab().getText()+".png");
						}

						Exporter.exportImage(image , tabviews.get(j).getGpaneHeight(), tabviews.get(j).getGpaneWidth(), path);

						return null;

					}
						};


						Thread export = new Thread(imageexport);
						export.start();

						counter++;
			}
			WritableImage image = results.exportImage();
			Task<Void> imageexport = new Task<Void>()
					{



				@Override
				protected Void call() {

					String path = file.getAbsolutePath();
					if(!path.endsWith(".png"))
					{
						path = path + "-heatmap" +".png";
					}
					else
					{
						path = path.replaceAll(".png", "-heatmap.png");
					}

					Exporter.exportImage(image , results.getHeight(), results.getWidth(), path);

					return null;

				}
					};




					Thread export = new Thread(imageexport);
					export.start();


					counter++;

					WritableImage image_peakData = pTC.exportImage();
					Task<Void> imageexportPeaks= new Task<Void>()
							{



						@Override
						protected Void call() {

							String path = file.getAbsolutePath();
							if(!path.endsWith(".png"))
							{
								path = path + "-result-charts" +".png";
							}
							else
							{
								path = path.replaceAll(".png", "-result-charts"+".png");
							}

							Exporter.exportImage(image_peakData , pTC.getHeight(), pTC.getWidth(), path);

							return null;

						}
							};


							export = new Thread(imageexportPeaks);
							export.start();

							counter++;

							WritableImage image_qualData = qualController.exportImage();
							Task<Void> imageexportBoxplots= new Task<Void>()
									{



								@Override
								protected Void call() {

									String path = file.getAbsolutePath();
									if(!path.endsWith(".png"))
									{
										path = path +  "-result-qualities.png";
									}
									else
									{
										path = path.replaceAll(".png", "-result-qualities.png");
									}

									Exporter.exportImage(image_qualData , qualController.getHeight(), qualController.getWidth(), path);

									return null;

								}
									};


									export = new Thread(imageexportBoxplots);
									export.start();



		}





	}

	@FXML
	public void loadMapper()
	{
		init();
		FileChooser fileChooser = new FileChooser();
		File file;
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Sierra datamapper files (*.dsierra", "*.dsierra");
		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.setTitle("Open sierra file");
		file = fileChooser.showOpenDialog(null);
		fileChooser = null;

		if (file != null) {
			FXMLDocumentController temp = this;
			System.err.println(file.getAbsolutePath());
			init();
			Importer.loadMapper(file.getAbsolutePath(),temp);
			mapperUpdate();
		}
	}


	@FXML
	public void saveMapper()
	{
		FileChooser fileChooser = new FileChooser();

		//Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Sierra datamapper files (*.dsierra", "*.dsierra");
		fileChooser.getExtensionFilters().add(extFilter);
		File file;
		fileChooser.setTitle("Save Sierra datamapper to file");
		file = fileChooser.showSaveDialog(null);
		if (file != null) {


			String path = file.getAbsolutePath();
			if(!path.endsWith(".dsierra"))
			{
				path +=".dsierra";
			}
			System.err.println(path);
			Exporter.exportMapper(path, mapper);
		}
	}

	@FXML
	public void createServer()
	{

		server = new ServerDialog();
		server.createServerDialog(this);
	}

	@FXML
	public void pullData()
	{
		if(peakCommander.isActive())
		{
			Object[] command = new Object[2];
			command[0] = "pullDataMapper";
			command[1] = peakCommander.getHash();
			peakCommander.sendCommand(command);
			peakCommander.ack();
		}
		else
		{
			System.out.println("Not active");
		}
	}

	@SuppressWarnings("unchecked")
	public void mapperUpdate()
	{
		for(Replicate replicate : mapper.getReplicates())
		{
			
			TreeItem<String> rep = new TreeItem<>(replicate.getName());
			TreeItem<String> experiment = new TreeItem<>("Experiment");
			TreeItem<String> background = new TreeItem<>("Background");

			TreeItem<String> experiment_path = new TreeItem<>(replicate.getExperiment().getDescription());
			TreeItem<String> background_path = new TreeItem<>(replicate.getBackground().getDescription());

			experiment.getChildren().add(experiment_path);
			background.getChildren().add(background_path);
			experiment.setExpanded(true);
			background.setExpanded(true);

			rep.getChildren().addAll(experiment, background);
			rep.setExpanded(true);
			treeView.getRoot().getChildren().add(rep);

			ReplicateTab tview = new ReplicateTab(replicate.getName());
			tabviews.add(tview);

			counter++;

		}

		offset.setText(String.valueOf(mapper.getOffset()));
		pcutoff.setText(String.valueOf(mapper.getPvaluecutoff()));
		windowsize.setText(String.valueOf(mapper.getWindowsize()));
		adjustCores.getValueFactory().setValue(mapper.getNumCores());
		if(mapper.getJobName() != null)
		{
			jobName.setText(mapper.getJobName());
		}
		else
		{
			jobName.setText(peakCommander.getHash().substring(0,7));
		}

		if(mapper.hasResults())
		{
			drawPlots(mapper);
		}

		progress.setProgress(1);
		dnaLabel.setVisible(false);
	}

	@FXML
	public void terminateConnection()
	{
		if(peakCommander.isActive())
		{
			Object[] c = {"QUIT","QUIT"};
			peakCommander.sendCommand(c);
			peakCommander.stopListener();
		}
		peakCommander.setActive(false);
	}

	@FXML
	public void pullBroad()
	{
		if(peakCommander.isActive())
		{
			String hash= peakCommander.getHash();
			Object[] c = {"pullBroad",hash};
			peakCommander.sendCommand(c);
			peakCommander.ack();
			receiver = new ReceiverDialog();
			receiver.init();
		}
	}

	@FXML
	public void pullNarrow()
	{
		if(peakCommander.isActive())
		{
			String hash= peakCommander.getHash();
			Object[] c = {"pullNarrow",hash};
			peakCommander.sendCommand(c);
			peakCommander.ack();
			receiver = new ReceiverDialog();
			receiver.init();

		}
	}

	@FXML
	public void SaveState()
	{
		if(peakCommander.isActive())
		{
			String hash= peakCommander.getHash();
			Object[] c = {"saveState",hash};
			peakCommander.sendCommand(c);
			peakCommander.ack();
		}
	}

	@FXML
	public void killBill()
	{
		if(peakCommander.isActive())
		{
			Object[] c = {"killJob","-"};
			peakCommander.sendCommand(c);
			peakCommander.ack();

		}
	}

	/** End FXML methods ********************************************************/

	public void init()
	{
		counter = 1;
		datasetsReady = 0;

		SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
		tab = selectionModel.getSelectedIndex();
		System.err.println("Selected tab is "+ tab);
		mKillBill.setDisable(true);
		mPullBroad.setDisable(true);
		mPullNarrow.setDisable(true);
		mSaveState.setDisable(true);

		//Clear TabPane
		while(tabPane.getTabs().size() > 1)
		{
			tabPane.getTabs().remove(1);
		}

		tabviews= new ArrayList<>();


		mapper = new DataMapper();
		//    	start.setDisable(true);
		offset.setText("50");
		windowsize.setText("200");
		pcutoff.setText("1.0E-5");

		//Init TreeView
		TreeItem<String> rootItem = new TreeItem<>("Data");
		treeView.setRoot(rootItem);
		rootItem.setExpanded(true);



		//Build a CellFactory for the Tree to implement a contextMenu
		treeView.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {

			@Override
			public TreeCell<String> call(TreeView<String> arg0) {
				// custom tree cell that defines a context menu for the root tree item
				return new MyTreeCell();
			}
		});


	}

	private void drawPlots(DataMapper mapper)
	{

		Platform.runLater(new Runnable() {


			@Override
			public void run() {

				int i=0;
				for(Replicate rep: mapper.getReplicates())
				{
					createReplicateTab(i, rep);
					i++;
				}

				if(i > 0)
				{

					results = new ResultTab("Summary");
					results.init(mapper.getReplicatePearsonCorrelation(), mapper, peakCommander);
					mKillBill.setDisable(true);
					mPullBroad.setDisable(false);
					mPullNarrow.setDisable(false);
					mSaveState.setDisable(false);
					tabPane.getTabs().add(results.getTab());

					Tab peakTab = new Tab("Peak information");

					FXMLLoader loader = new FXMLLoader();
					try {
						Parent root = (Parent) loader.load(getClass().getResourceAsStream("peakTab.fxml"));
						peakTab.setContent(root);
					} catch (IOException e) {
						e.printStackTrace();
					}
					pTC = loader.getController();
					tabPane.getTabs().add(peakTab);
					pTC.drawPlots(mapper);

					Tab qualityTab = new Tab("Quality information");

					FXMLLoader qualLoader = new FXMLLoader();
					try {
						Parent root = (Parent) qualLoader.load(getClass().getResourceAsStream("qualityTab.fxml"));
						qualityTab.setContent(root);
					} catch (IOException e) {
						e.printStackTrace();
					}
					qualController = qualLoader.getController();
					tabPane.getTabs().add(qualityTab);
					qualController.initPlots(mapper);

				}

				SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
				selectionModel.select(tab);
			}
		});

	}

	class MyTreeCell extends TextFieldTreeCell<String> {
		private ContextMenu rootContextMenu;
		private ContextMenu pathContextMenu;
		private TextField textField;
		private String oldText;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MyTreeCell() {
			// instantiate the context menu
			this.setEditable(false);
			rootContextMenu = new ContextMenu();
			MenuItem renameReplicate = new MenuItem("Rename Replicate");
			MenuItem deleteReplicate = new MenuItem("Delete Replicate");
			rootContextMenu.getItems().add(renameReplicate);
			rootContextMenu.getItems().add(deleteReplicate);

			renameReplicate.setOnAction(new EventHandler() {

				@Override
				public void handle(Event event) {
					oldText = getTreeItem().getValue();
					edit();
				}

			});
			pathContextMenu = new ContextMenu();
			MenuItem editPath = new MenuItem("Edit Path");

			pathContextMenu.getItems().add(editPath);

			editPath.setOnAction(new EventHandler() {

				@Override
				public void handle(Event event) {
					edit();

				}

			});

			deleteReplicate.setOnAction(new EventHandler() {

				@Override
				public void handle(Event event) {
					int index = getTreeItem().getParent().getChildren().indexOf(getTreeItem());
					System.err.println(getTreeItem().getChildren().get(0).getValue());
					//Adjust disabler variable
					if(getTreeItem().getChildren().get(0).getChildren().isEmpty())
					{
						datasetsReady -= 1;
					}
					if(getTreeItem().getChildren().get(1).getChildren().isEmpty())
					{
						datasetsReady -= 1;
					}
					mapper.getReplicates().remove(index);

					//Delete Replicate
					getTreeItem().getParent().getChildren().remove(index);
					tabviews.remove(index);

				}
			});

		}
		private void edit()
		{
			this.setEditable(true);
			this.startEdit();
			if (textField == null ) {
				createTextField();
			}
			setText(null);
			setGraphic(textField);
			textField.selectAll();
			this.setEditable(false);
		}

		private void createTextField() {
			textField = new TextField(getString());
			textField.setOnKeyReleased(new EventHandler<KeyEvent>() {

				@Override
				public void handle(KeyEvent t) {
					if (t.getCode() == KeyCode.ENTER) {
						commitEdit(textField.getText());
						if(oldText != null)
						{
							for(ReplicateTab tab : tabviews)
							{
								if(tab.getTab().getText().equals(oldText))
								{
									tab.getTab().setText(getTreeItem().getValue());
									while(tabPane.getTabs().size() > 1)
									{
										tabPane.getTabs().remove(1);
									}
									
									ObservableList<TreeItem<String>> treereps = treeView.getRoot().getChildren();
									for(int i = 0; i < treereps.size(); i++)
									{
										mapper.getReplicates().get(i).setName(treereps.get(i).getValue());								
									}
									if(mapper.hasResults())
									{
									drawPlots(mapper);
									}
								}
							}
							oldText = null;
						}
					} else if (t.getCode() == KeyCode.ESCAPE) {
						cancelEdit();
					}
				}

			});


		}
		private String getString() {
			return getItem() == null ? "" : getItem().toString();
		}

		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);

			if (!empty && getTreeItem().getParent().getValue().contains("Data")) 
			{
				setContextMenu(rootContextMenu);
			}
			if(!empty && (getTreeItem().getParent().getValue().contains("Experiment") || getTreeItem().getParent().getValue().contains("Background")))
			{
				setContextMenu(pathContextMenu);
			}


		}
	}

	public void setDataMapper(DataMapper mapper)
	{
		this.mapper = mapper;
	}

	public PeakCommander getPeakCommander() {
		return peakCommander;
	}

	public void setPeakCommander(PeakCommander peakCommander) {
		this.peakCommander = peakCommander;
	}

	public void setProgress(Double progressState) {
		if(!dnaLabel.isVisible())
		{
			dnaLabel.setVisible(true);
			mKillBill.setDisable(false);
			mPullBroad.setDisable(true);
			mPullNarrow.setDisable(true);
			mSaveState.setDisable(true);
		}
		progress.setProgress(progressState);
		lengthValue = distLength - lengthValue;

		pacMan.setLength(270+ lengthValue);

		startAngle = distAngle - startAngle;

		pacMan.setStartAngle(15 + startAngle);

		if(lengthValue == 60)
		{
			dnaLabel.setText(dna.getNucleotide(dnaLabel.getText()));
		}
		if(results != null)
		{
			results.setProgress(progressState);
		}
	}

	public DataMapper getDataMapper()
	{
		return mapper;
	}

	private void createReplicateTab(int i, Replicate rep)
	{
		tabPane.getTabs().add(tabviews.get(i).getTab());

		int max = rep.getExperiment().getHistogram().getBinXValue().get(rep.getExperiment().getHistogram().getBinXValue().size()-1);
		int background = rep.getBackground().getHistogram().getBinXValue().get(rep.getBackground().getHistogram().getBinXValue().size()-1);

		if(max < background)
		{
			max = background;
		}


		tabviews.get(i).initTab(rep.getLeastSquareDist(), max,rep.getMaxPValueExp());
		double range = 0;



		if(rep.getExperiment().getLambdaRaw() < rep.getBackground().getLambdaRaw())
		{
			PoissonDistribution ptest = new PoissonDistribution(rep.getBackground().getLambdaRaw());
			range = ptest.inverseCumulativeProbability(0.9999);
		}
		else
		{
			PoissonDistribution ptest = new PoissonDistribution(rep.getExperiment().getLambdaRaw());
			range = ptest.inverseCumulativeProbability(0.9999);
		}

		tabviews.get(i).addExperimentLambda(rep.getExperiment().getLambda(),range);
		tabviews.get(i).addBackgroundLambda(rep.getBackground().getLambda(),range);

		tabviews.get(i).addRawExperimentLambda(rep.getExperiment().getLambdaRaw(),range);
		tabviews.get(i).addRawBackgroundLambda(rep.getBackground().getLambdaRaw(),range);

		tabviews.get(i).addRawExperimentHistogram(rep.getExperiment().getHistogram());
		tabviews.get(i).addRawBackgroundHistogram(rep.getBackground().getHistogram());

		tabviews.get(i).createBoxPlot(rep.getName(), rep);

		tabviews.get(i).addPvalueData(rep, mapper);

		tabviews.get(i).addsSigHistData(rep, mapper, rep.getName());

	}

	public void setBusy(boolean disabled)
	{
		start.setDisable(disabled);
	}

	private boolean isInteger(String value) {
		if (value == null) {
			return false;
		}
		try {
			new Integer(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public ReceiverDialog getReceiver() {
		return receiver;
	}

	public void setReceiver(ReceiverDialog receiver) {
		this.receiver = receiver;
	}

}