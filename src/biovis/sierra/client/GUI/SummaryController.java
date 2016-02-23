
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
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import biovis.sierra.client.Commander.PeakCommander;
import biovis.sierra.client.GUI.GUIHelper.SpinnerValue;
import biovis.sierra.client.GUI.GUIHelper.ToggleSwitch;
import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;
import biovis.sierra.data.IO.ExportSnapshot;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
/**
*
* @author Daniel Gerighausen
*/
public class SummaryController  implements Initializable {


    private GridPane heatmap;
    private DataMapper mapper;



    private final double min = -1.0;
    private final double max = 1.0;
    private int counter;

    @FXML
    private GridPane heatPane;
    @FXML
    private GridPane resultPane;
    @FXML
    private CheckBox correctCorrelations;
    @FXML
    private CheckBox weights;

    ArrayList<Spinner<Double>> spinnerList = new ArrayList<Spinner<Double>>();



    private Button restart;



    private ProgressBar progress;
//    @FXML
    private GridPane colHeader;
//    @FXML
    private GridPane rowHeader;
    @FXML
    private StackPane colPanel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        heatPane.autosize();


    }

    public void printReplicates(DataMapper mapper, PeakCommander pc)
    {
        this.mapper = mapper;
        int nrReplicates = mapper.getReplicates().size();
        counter = nrReplicates;
        for(int i= 0; i < nrReplicates; i++)
        {
            Replicate rep = mapper.getReplicates().get(i);
            RowConstraints column1 = new RowConstraints();
//            column1.setPercentHeight(100.0/(nrReplicates+1));
            column1.setPrefHeight(50);
            column1.setMinHeight(50);
            //            System.err.println(100.0/nrReplicates+1);

            Label repname = new Label(mapper.getReplicates().get(i).getName()+":");
            resultPane.add(repname, 0, i);


 System.err.println(rep.getLeastSquareDist());
            SpinnerValue factory = new SpinnerValue(0.00, 100000.000, rep.getWeight());
            Spinner<Double> adjustWeight = new Spinner<Double>(factory);
            //            adjustWeight.set
 adjustWeight.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (isDouble(newValue)) {
                        adjustWeight.getValueFactory().setValue( Double.parseDouble(newValue));
                    }
                } catch (NumberFormatException e) {
                    if (isDouble(oldValue)) {
 adjustWeight.getValueFactory().setValue(Double.parseDouble(oldValue));
                    }
                }
            });

            spinnerList.add(adjustWeight);
            adjustWeight.setEditable(true);
            adjustWeight.valueProperty().addListener(
                    (obs, oldValue, newValue) -> rep.setWeight(newValue));


            resultPane.add(adjustWeight, 1, i);


            ToggleSwitch switchoff = new ToggleSwitch(rep.isActive());
            if(!rep.isActive())
            {
                counter--;
            }
            switchoff.switchOnProperty().addListener(
                    (obs, oldValue, newValue) -> disableReplicate(rep, newValue));
            resultPane.add(switchoff, 2, i);
            resultPane.getRowConstraints().add(column1);

        }

        Separator sep = new Separator();
        resultPane.add(sep, 1, nrReplicates);
//        nrReplicates++;

        RowConstraints column1 = new RowConstraints();

//        column1.setPercentHeight(100.0/(nrReplicates+1));
        column1.setPrefHeight(50);
        column1.setMinHeight(50);
//        CheckBox disableWeights = new CheckBox("Disable weights");
        weights.setSelected(true);
        mapper.setWeighted(true);
        weights.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                if(!weights.isSelected())
                {
                    for(Spinner<Double> spinner : spinnerList)
                    {
                        spinner.setDisable(true);
                    }
                    mapper.setWeighted(false);
                }
                else
                {
                    for(Spinner<Double> spinner : spinnerList)
                    {
                        spinner.setDisable(false);
                    }
                    mapper.setWeighted(true);

                }
            }
        });

//        resultPane.add(disableWeights, 0, nrReplicates);


        CheckBox cQuality = new CheckBox("Enable quality counting");
        mapper.setQualityCounting(false);
        cQuality.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(cQuality.isSelected())
                {

                    mapper.setQualityCounting(true);
                }
                else
                {

                    mapper.setQualityCounting(false);

                }
            }
        });

        resultPane.add(cQuality, 0, nrReplicates+1);

    Label qValueLabel = new Label("q-Value correction method:");
//    qValueLabel.setAlignment(Pos.CENTER_RIGHT);
    HBox qvalueBox = new HBox(qValueLabel);
    qvalueBox.setAlignment(Pos.CENTER_RIGHT);
    resultPane.add(qvalueBox, 1, nrReplicates+1);


    ChoiceBox<String>    qValueMenu = new ChoiceBox<String>(FXCollections.observableArrayList(
            "Holm Bonferroni", "Storey Simple", "Storey Bootstrap"));
    qValueMenu.setValue(mapper.getQValueMethod());
    resultPane.add(qValueMenu, 2, nrReplicates+1);
     restart = new Button("Recalculate");

        restart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                mapper.setQValueMethod(qValueMenu.getValue());
                Object[] c = new Object[2];
                c[0] = "setDataMapper";

                Gson gson = new Gson();
                String dataString = gson.toJson(mapper);
                c[1] = dataString;
                pc.sendCommand(c);


                Object[] command = {"recalc","recalc"};
                pc.sendCommand(command);
                pc.ack();
                progress.setProgress(0.0);
            }
        });


        resultPane.add(restart, 2, nrReplicates+2);


        progress =  new ProgressBar();
        progress.setProgress(1);
        progress.setStyle("-fx-accent:green");

        HBox progressBox = new HBox(progress);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        resultPane.add(progressBox, 1, nrReplicates+2);

        resultPane.getRowConstraints().add(column1);
        resultPane.getRowConstraints().add(column1);
        resultPane.getRowConstraints().add(column1);

        correctCorrelations.setSelected(true);

        correctCorrelations.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(correctCorrelations.isSelected())
                {

                    mapper.setCorrectCorrelation(true);
                }
                else
                {
                    mapper.setCorrectCorrelation(false);
                }
            }
        });


    }


    private void disableReplicate(Replicate rep, boolean value)
    {
//        System.err.println(value);
        rep.setActive(value);
        if(value)
        {
            counter++;
            if(counter > 0)
            {
                restart.setDisable(false);
            }
        }
        else
        {
            counter--;
            if(counter < 1)
            {
                restart.setDisable(true);
            }
        }

    }





    private boolean isDouble(String value) {
          if (value == null) {
                return false;
            }
            try {
                new Double(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }

}
public void draw(double[][] correlations)
    {


        double columns = 400.0 / correlations.length;
        heatmap = new GridPane();
        colHeader = new GridPane();
        colHeader.setGridLinesVisible(true);
        
        rowHeader = new GridPane();
        rowHeader.setGridLinesVisible(true);
       
        ColumnConstraints headerCol = new ColumnConstraints();
        headerCol.setPrefWidth(columns);
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(columns);;
        

        RowConstraints header = new RowConstraints();
        header.setPrefHeight(colPanel.getPrefHeight());
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPrefWidth(columns);
        RowConstraints row1 = new RowConstraints();
        row1.setPrefHeight(columns);
        for(int i = 0; i < correlations.length; i++)
        {
            colHeader.getRowConstraints().add(headerRow);
            heatmap.getColumnConstraints().add(column1);
            heatmap.getRowConstraints().add(row1);
            rowHeader.getRowConstraints().add(headerRow);

        }
        heatmap.setStyle("-fx-background-color: BLACK; -fx-padding: 1;"
                 +"-fx-hgap: 1; -fx-vgap: 1;");
        drawTiles(correlations);

        heatmap.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
                {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.SECONDARY) {
                    WritableImage snapshot = heatPane.snapshot(new SnapshotParameters(), null);
                    ExportSnapshot.exportImage(snapshot, heatPane);
                    event.consume();
                }

            }
                }
                );
       
        heatPane.add(heatmap, 1, 1);
        colPanel.getChildren().add(colHeader);
        colHeader.setAlignment(Pos.CENTER_RIGHT);
        rowHeader.setAlignment(Pos.CENTER_RIGHT);
        heatPane.add(rowHeader, 0 , 1);

    }

    private void drawTiles(double[][] correlations)
    {
        Color back =  Color.rgb(244,244,244);
        VBox zero = new VBox();
        zero.setBackground(new Background(new BackgroundFill(back,CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
        for(int i= 0; i < correlations.length;i++)
        {
            StackPane replicatecol = new StackPane();
            replicatecol.setBackground(new Background(new BackgroundFill(back,CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
            Label bla = new Label();
           
        
            bla.setText(" " +mapper.getReplicates().get(i).getName()+ "  ");
            replicatecol.getChildren().add(bla);
            replicatecol.setAlignment(Pos.CENTER_RIGHT);
        
            colHeader.add(replicatecol, 0, correlations.length-i-1);

            StackPane replicaterow = new StackPane();
            replicaterow.setBackground(new Background(new BackgroundFill(back,CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
            replicaterow.setAlignment(Pos.CENTER_RIGHT);
            replicaterow.getChildren().add(new Text(" " +mapper.getReplicates().get(i).getName()+ "  "));

            rowHeader.add(replicaterow, 0, i);
        }

        for(int i =0; i < correlations.length; i++)
        {
            for(int j = 0; j < correlations[i].length; j++)
            {
                StackPane pane = new StackPane();
                Tooltip tp = new Tooltip("Correlation between "+ mapper.getReplicates().get(i).getName() +" and "+ mapper.getReplicates().get(j).getName() +": "+correlations[i][j]);
                Tooltip.install(pane, tp);
                pane.setBackground(new Background(new BackgroundFill(getColorForValue(correlations[i][j]), CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
                heatmap.add(pane, i, j);

            }
        }

    }



    private Color getColorForValue(Double value) {
        if (value < min || value > max || value.isNaN()) {
            return Color.BLACK ;
        }
        if(value >=0.0)
        {

            return Color.hsb(0,value,1.0 );
        }
        else{
            return Color.hsb(Color.BLUE.getHue(), (-1.0 * value), 1.0);
        }
    }


    public GridPane getHeatMap()
    {
        return heatPane;
    }

    public void setProgress(Double progressState) {
        progress.setProgress(progressState);

    }

}

