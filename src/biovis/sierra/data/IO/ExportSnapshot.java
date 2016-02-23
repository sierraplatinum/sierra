/**
 *****************************************************************************
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
 ******************************************************************************
 */
package biovis.sierra.data.IO;

import java.io.File;

import javafx.concurrent.Task;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

/**
 *
 * @author Daniel Gerighausen
 */
public class ExportSnapshot {

    public static void exportImage(WritableImage snapshot, Pane pane) {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("png files (*.png", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);
        File file;
        fileChooser.setTitle("Save figure as png file");
        file = fileChooser.showSaveDialog(null);
        if (file != null) {

            Task<Void> imageexport = new Task<Void>() {

                @Override
                protected Void call() {

                    String path = file.getAbsolutePath();
                    if (!path.endsWith(".png")) {
                        path = path + ".png";
                    }

                    Exporter.exportImage(snapshot, (int) pane.getHeight(), (int) pane.getWidth(), path);

                    return null;
                }
            };

            Thread export = new Thread(imageexport);
            export.start();
        }
    }
}
