package de.emaeuer.gui.controller.util;

import com.brunomnsilva.smartgraph.graph.DigraphEdgeList;
import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graphview.*;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;
import de.emaeuer.state.value.GraphStateValue.Connection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class StateValueOutputMapper {

    private static final Logger LOG = LogManager.getLogger(StateValueOutputMapper.class);

    private final Map<String, Object> visualRepresentations = new HashMap<>();
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

    // FIXME nodes in graph lay on top of each other
    private void refreshGraphView(StateParameter<?> stateType, GraphStateValue graphState, String suffix) {
        if (!graphState.changedSinceLastGet() || graphState.getValue() == null || graphState.getValue().isEmpty()) {
            return;
        }

        String mapIdentifier = createMapIdentifier(stateType, suffix + ".view");
        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createGraphView(stateType, suffix);
        }

        //noinspection unchecked if creation works properly issues with casting not possible
        GraphView<String, Double> graphView = (GraphView<String, Double>) this.visualRepresentations.get(mapIdentifier);
        //noinspection unchecked if creation works properly issues with casting not possible
        Graph<String, Double> graph = (Graph<String, Double>) this.visualRepresentations.get(createMapIdentifier(stateType, suffix + ".graph"));

        // completely reset the graph --> update would be possible but harder to implement and may not save time
        graph.edges().forEach(graph::removeEdge);
        graph.vertices().forEach(graph::removeVertex);

        Set<String> existingVertices = new HashSet<>();

        for (Connection connection : graphState.getValue()) {
            if (existingVertices.add(connection.start())) {
                graph.insertVertex(connection.start());
            };
            if (existingVertices.add(connection.target())) {
                graph.insertVertex(connection.target());
            }
            Edge<Double, String> edge = graph.insertEdge(connection.start(), connection.target(), connection.weight());
            graphView.setStyle(edge, getEdgeStyle(connection.weight()));
        }

        graphView.update();
    }

    private String getEdgeStyle(double weight) {
        int red = (int) Math.round(-127 * weight + 127);
        int green = (int) Math.round(127 * weight + 127);
        int blue = (int) Math.round(-127 * Math.abs(weight) + 127);

        return String.format("-fx-stroke: rgb(%d, %d, %d);", red, green, blue);
    }

    private void createGraphView(StateParameter<?> stateType, String suffix) {
        SmartGraphProperties properties = null;
        try (FileInputStream input = new FileInputStream("particle_environment_launcher/src/main/resources/gui/graph/smartgraph.properties")) {
            properties = new SmartGraphProperties(input);
        } catch (IOException e) {
           LOG.warn("Failed to load graph configuration using default one", e);
        }

        Graph<String, Double> graph = new DigraphEdgeList<>();
        GraphView<String, Double> graphView = new GraphView<>(graph, properties, null, this.getClass().getResource("/gui/graph/graph.css").toExternalForm());

        this.visualRepresentations.put(createMapIdentifier(stateType, suffix + ".view"), graphView);
        this.visualRepresentations.put(createMapIdentifier(stateType, suffix + ".graph"), graph);
        this.newContent.add(createCard(stateType, graphView));

        graphView.setMinWidth(250);
        graphView.setMinHeight(250);

        graphView.init();
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
