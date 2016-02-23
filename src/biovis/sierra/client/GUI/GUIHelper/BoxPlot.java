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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import biovis.sierra.data.Replicate;
import biovis.sierra.data.peakcaller.IntBoxplotData;

/**
 *
 * @author Daniel Gerighausen
 */
public class BoxPlot extends JPanel {

    private static final long serialVersionUID = -5318183620885802996L;
    private static final int strokeWidth = 3;

    public JFreeChart chart;

    /**
     * Constructor.
     * @param title
     * @param replicate
     * @param rep
     */
    @SuppressWarnings("deprecation")
    public BoxPlot(
            final String title,
            String replicate,
            Replicate rep
    ) {
        final BoxAndWhiskerCategoryDataset dataset = createSampleDataset(replicate, rep);
        createBoxplot(title, dataset);
    }

    /**
     * Constructor.
     * @param title
     * @param replicate
     * @param experimentData
     * @param backgroundData 
     */
    public BoxPlot(
            String title,
            String replicate,
            IntBoxplotData experimentData,
            IntBoxplotData backgroundData
    ) {
            final BoxAndWhiskerCategoryDataset dataset = createSampleDataset(replicate, experimentData, backgroundData);
            createBoxplot(title, dataset);
    }

    /**
     * create boxplot
     *
     * @param title title
     * @param dataset data set
     */
    private void createBoxplot(
            String title,
            final BoxAndWhiskerCategoryDataset dataset
    ) {
        final CategoryAxis xAxis = new CategoryAxis("Type");
        final NumberAxis yAxis = new NumberAxis("Phred score");
        yAxis.setRange(-5, 45);
        final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setMeanVisible(false);
        renderer.setFillBox(false);
        //        renderer.setArtifactPaint(Color.BLACK);
        renderer.setSeriesPaint(0, Color.GRAY);
        renderer.setSeriesOutlinePaint(0, Color.GRAY);
        renderer.setUseOutlinePaintForWhiskers(true);
        renderer.setOutlineStroke(new BasicStroke(strokeWidth));
        renderer.setStroke(new BasicStroke(strokeWidth));
        //can be fixed since we are visualizing phred scores

        java.awt.Image im = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("biovis/sierra/client/GUI/GUIHelper/boxplotbackground.png"));

        final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

        chart = new JFreeChart(
                title,
                new Font("SansSerif", Font.PLAIN, 14),
                plot,
                true
        );

        //Set the background to transparent
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));
        plot.setBackgroundPaint(null);
        plot.setBackgroundImage(im);
    }

    private BoxAndWhiskerCategoryDataset createSampleDataset(
            String replicate,
            IntBoxplotData experimentData,
            IntBoxplotData backgroundData
    ) {
        final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

        BoxAndWhiskerItem experiment = new BoxAndWhiskerItem(
                -100,
                experimentData.getMedian(),
                experimentData.getLowerQuartile(),
                experimentData.getUpperQuartile(),
                experimentData.getMin(),
                experimentData.getMax(),
                null, null, null);

        dataset.add(experiment, replicate, " Experiment");

        BoxAndWhiskerItem background = new BoxAndWhiskerItem(
                -100,
                backgroundData.getMedian(),
                backgroundData.getLowerQuartile(),
                backgroundData.getUpperQuartile(),
                backgroundData.getMin(),
                backgroundData.getMax(),
                null, null, null);
        dataset.add(background, replicate, " Background");

        return dataset;

    }

    /**
     * Creates a sample dataset.
     *
     * @return A sample dataset.
     */
    private BoxAndWhiskerCategoryDataset createSampleDataset(
            String replicate,
            Replicate rep
    ) {

        final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

        BoxAndWhiskerItem experiment = new BoxAndWhiskerItem(
                -100,
                rep.getExperiment().getMedianQuality(),
                rep.getExperiment().getLowerQualityQuartile(),
                rep.getExperiment().getUpperQualityQuartile(),
                rep.getExperiment().getMinQuality(),
                rep.getExperiment().getMaxQuality(),
                null, null, null);

        dataset.add(experiment, replicate, " Experiment");

        BoxAndWhiskerItem background = new BoxAndWhiskerItem(
                -100,
                rep.getBackground().getMedianQuality(),
                rep.getBackground().getLowerQualityQuartile(),
                rep.getBackground().getUpperQualityQuartile(),
                rep.getBackground().getMinQuality(),
                rep.getBackground().getMaxQuality(),
                null, null, null);
        dataset.add(background, replicate, " Background");

        return dataset;
    }
}
