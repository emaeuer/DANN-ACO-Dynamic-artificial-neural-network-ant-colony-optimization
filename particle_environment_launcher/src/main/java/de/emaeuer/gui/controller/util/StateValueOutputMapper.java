package de.emaeuer.gui.controller.util;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class StateValueOutputMapper {

    private final Map<String, Node> visualRepresentations = new HashMap<>();
    private final StateHandler<?> state;

    private final List<VBox> newContent = new ArrayList<>();

    public StateValueOutputMapper(StateHandler<?> state) {
        this.state = state;
    }

    public List<Node> refreshProperties() {
        if (this.state == null) {
            return Collections.emptyList();
        }

        state.getCurrentState()
                .forEach((key, value) -> refreshPropertyOfState(key, value, ""));

        List<Node> copy = new ArrayList<>(this.newContent);
        this.newContent.clear();
        return copy;
    }

    private void refreshPropertyOfState(StateParameter<?> stateType, AbstractStateValue<?, ?> stateValue, String suffix) {
        if (stateValue instanceof DataSeriesStateValue dataSeries) {
            refreshPlotForDataSeries(stateType, dataSeries, suffix);
        } else if (stateValue instanceof EmbeddedState embeddedState) {
            refreshPaneForEmbeddedState(embeddedState);
        } else if (stateValue instanceof NumberStateValue numberState) {
            refreshFieldForNumber(stateType, numberState, suffix);
        } else if (stateValue instanceof MapOfStateValue mapState) {
            refreshSelectionForStateValue(stateType, mapState, suffix);
        } else if (stateValue instanceof GraphStateValue graphState) {
            refreshGraphView(stateType, graphState, suffix);
        }
    }

    // *********************************************
    // Methods for handling data series
    // *********************************************

    private void refreshPlotForDataSeries(StateParameter<?> stateType, DataSeriesStateValue dataSeries, String suffix) {
        if (!dataSeries.changedSinceLastGet()) {
            return;
        }

        String mapIdentifier = createMapIdentifier(stateType, suffix);

        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createPlot(stateType, suffix);
        }

        //noinspection unchecked no safe way to cast generic --> if generation on top works correctly is always valid cast
        LineChart<Number, Number> chart = (LineChart<Number, Number>) this.visualRepresentations.get(mapIdentifier);

        Map<String, List<Double[]>> dataWithoutSeries = updateDataSeries(dataSeries, chart.getData());
        createDataSeries(dataWithoutSeries, chart.getData());
    }

    private void createPlot(StateParameter<?> stateType, String suffix) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Evaluation number");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fitness");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);

        chart.getStyleClass().add("output_plot");
        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), chart);


        VBox box = createCard(stateType, chart);
        box.getStyleClass().add("output_plot_background");

        this.newContent.add(box);
    }

    private Map<String, List<Double[]>> updateDataSeries(DataSeriesStateValue dataSeries, ObservableList<Series<Number, Number>> property) {
        // create copy of map to enable modification without change to original
        Map<String, List<Double[]>> seriesData = new HashMap<>(dataSeries.getValue());
        for (Series<Number, Number> series : property) {
            List<Double[]> data = seriesData.remove(series.getName());
            int seriesSize = series.getData().size();

            data.subList(seriesSize, data.size())
                    .stream()
                    .map(p -> new XYChart.Data<Number, Number>(p[0], p[1]))
                    .forEach(d -> series.getData().add(d));
        }
        // map only contains series that were not created yet
        return seriesData;
    }


    private void createDataSeries(Map<String, List<Double[]>> seriesData, ObservableList<Series<Number,Number>> property) {
        // create series for each entry
        for (Entry<String, List<Double[]>> singleSeriesData : seriesData.entrySet()) {
            ObservableList<XYChart.Data<Number, Number>> data = FXCollections.observableArrayList();

            singleSeriesData.getValue()
                    .stream()
                    .map(p -> new XYChart.Data<Number, Number>(p[0], p[1]))
                    .forEach(data::add);

            Series<Number, Number> series = new Series<>(singleSeriesData.getKey(), data);
            property.add(series);
        }
    }

    // *********************************************
    // Methods for handling number data
    // *********************************************

    private void refreshFieldForNumber(StateParameter<?> stateType, NumberStateValue numberState, String suffix) {
        if (!numberState.changedSinceLastGet()) {
            return;
        }

        String mapIdentifier = createMapIdentifier(stateType, suffix);
        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createFieldForNumber(stateType, numberState, suffix);
        }

        Label label = (Label) this.visualRepresentations.get(mapIdentifier);
        label.setText(numberState.getValue().toString());
    }

    private void createFieldForNumber(StateParameter<?> stateType, NumberStateValue numberState, String suffix) {
        Label label = new Label(numberState.getValue().toString());
        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), label);
        label.getStyleClass().add("number_field");

        this.newContent.add(createCard(stateType, label));
    }

    // *********************************************
    // Methods for handling embedded data
    // *********************************************

    private void refreshPaneForEmbeddedState(EmbeddedState embeddedState) {
        if (embeddedState == null || embeddedState.getValue() == null) {
            return;
        }

        embeddedState.getValue()
                .getCurrentState()
                .forEach((key, value) -> refreshPropertyOfState(key, value, "")); //refresh all components
    }

    // *********************************************
    // Methods for handling map data
    // *********************************************

    private void refreshSelectionForStateValue(StateParameter<?> stateType, MapOfStateValue mapState, String suffix) {
        String mapIdentifier = createMapIdentifier(stateType, suffix);
        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createSelectionForStateValue(stateType, suffix);
        }

        VBox box = (VBox) this.visualRepresentations.get(mapIdentifier);
        //noinspection unchecked
        ComboBox<String> selection = (ComboBox<String>) box.getChildren().get(1);
        StackPane stack = (StackPane) box.getChildren().get(2);

        mapState.getValue()
                .forEach((key, value) -> {
                    VBox node = refreshSelectablePane(stateType, selection, key, value);
                    // node is not null if it was created during refresh
                    if (node != null) {
                        // remove first element because it is the title which is not necessary for a selection
                        node.getChildren().remove(0);
                        stack.getChildren().add(node);
                    }
                });
    }

    private void createSelectionForStateValue(StateParameter<?> stateType, String suffix) {
        ComboBox<String> selection = new ComboBox<>();
        StackPane stack = new StackPane();
        VBox box = createCard(stateType, selection, stack);

        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), box);
        this.newContent.add(box);
    }

    private VBox refreshSelectablePane(StateParameter<?> stateType, ComboBox<String> selection, String key, AbstractStateValue<?, ?> value) {
        int newContentSize = this.newContent.size();
        refreshPropertyOfState(stateType, value, key);

        // check if a new element was created
        if (newContentSize == this.newContent.size()) {
            // nothing was created --> element already existed
            return null;
        } else {
            // something was created --> element is new
            // remove from new content because it isn't an individual card
            VBox newElement = this.newContent.remove(newContentSize);

            selection.getItems().add(key);
            if (selection.getValue() == null) {
                selection.setValue(key);
            }

            // show on selection of corresponding key
            selection.valueProperty().addListener((v, o, n) -> {
                if (key.equals(n)) {
                    newElement.toFront();
                }
            });

            return newElement;
        }
    }

    // *********************************************
    // Methods for handling graph data
    // *********************************************

    // FIXME graph view ugly and method not refactored
    private void refreshGraphView(StateParameter<?> stateType, GraphStateValue graphState, String suffix) {
        if (!graphState.changedSinceLastGet() || graphState.getValue() == null || graphState.getValue().edgeSet().isEmpty()) {
            return;
        }

        String mapIdentifier = createMapIdentifier(stateType, suffix);
        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createGraphView(stateType, suffix);
        }

        ImageView view = (ImageView) this.visualRepresentations.get(mapIdentifier);

        Graph<String, DefaultWeightedEdge> graph = graphState.getValue();
        JGraphXAdapter<String, DefaultWeightedEdge> graphAdapter = new JGraphXAdapter<>(graph);
        mxIGraphLayout layout = new mxCircleLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());

        BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File("temp/graph.jpg");
        try {
            ImageIO.write(image, "JPG", imgFile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try (FileInputStream in = new FileInputStream(imgFile)) {
            Image img = new Image(in);
            view.setImage(img);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGraphView(StateParameter<?> stateType, String suffix) {
        ImageView view = new ImageView();

        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), view);
        this.newContent.add(createCard(stateType, view));
    }

    // *********************************************
    // Util methods
    // *********************************************

    private static String createMapIdentifier(StateParameter<?> stateType, String suffix) {
        return String.join("-", stateType.getClass().getName(), stateType.getKeyName(), suffix);
    }

    private Label createCardTitle(StateParameter<?> stateType) {
        Label title =  new Label(stateType.getName());
        title.getStyleClass().add("card_title");

        return title;
    }

    private VBox createCard(StateParameter<?> stateType, Node... elements) {
        VBox box = new VBox();
        box.setSpacing(10);

        box.getChildren().add(createCardTitle(stateType));
        box.getChildren().addAll(elements);
        box.getStyleClass().add("output_card");

        return box;
    }
}
