package de.emaeuer.gui.controller.util;

import com.brunomnsilva.smartgraph.graph.DigraphEdgeList;
import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graphview.*;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;
import de.emaeuer.state.value.data.DataPoint;
import de.emaeuer.state.value.GraphStateValue.Connection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

public class StateValueOutputMapper {

    private static final Logger LOG = LogManager.getLogger(StateValueOutputMapper.class);

    private final Map<String, Object> visualRepresentations = new HashMap<>();
    private final StateHandler<?> state;

    private final List<VBox> newContent = new ArrayList<>();

    public StateValueOutputMapper(StateHandler<?> state) {
        this.state = state;
    }

    public synchronized List<Node> refreshProperties() {
        if (this.state == null) {
            return Collections.emptyList();
        }

        state.execute(t -> state.getCurrentState()
                .forEach((key, value) -> refreshPropertyOfState(key, value, "")));

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
        } else if (stateValue instanceof DistributionStateValue distributionState) {
            refreshDistributionState(stateType, distributionState, suffix);
        } else if (stateValue instanceof ScatteredDataStateValue scatteredValue) {
            refreshScatteredDataState(stateType, scatteredValue, suffix);
        } else if (stateValue instanceof CollectionDistributionStateValue collectionValue) {
            refreshCollectionDistributionState(stateType, collectionValue, suffix);
        } else if (stateValue instanceof DataQuantityStateValue quantityValue) {
            refreshPieChartForQuantity(stateType, quantityValue, suffix);
        }
    }

    // *********************************************
    // Methods for handling data series
    // *********************************************

    private void refreshPlotForDataSeries(StateParameter<?> stateType, DataSeriesStateValue dataSeries, String suffix) {
        String mapIdentifier = createMapIdentifier(stateType, suffix);

        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createLinePlot(dataSeries, stateType, suffix);
        }

        //noinspection unchecked no safe way to cast generic --> if generation on top works correctly is always valid cast
        XYChart<Number, Number> chart = (XYChart<Number, Number>) this.visualRepresentations.get(mapIdentifier);
        updateDataSeries(dataSeries, chart.getData());
    }

    private void createLinePlot(DataSeriesStateValue dataSeries, StateParameter<?> stateType, String suffix) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Evaluation number");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(stateType.getName());
        XYChart<Number, Number> chart;

        if (dataSeries instanceof CumulatedDataSeriesStateValue) {
            chart = new StackedAreaChart<>(xAxis, yAxis);
            ((StackedAreaChart<Number, Number>) chart).setCreateSymbols(false);
        } else {
            chart = new LineChart<>(xAxis, yAxis);
            ((LineChart<Number, Number>) chart).setCreateSymbols(false);
        }

        chart.setAnimated(false);
        chart.getStyleClass().add("output_plot");

        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), chart);

        VBox box = createCard(stateType, chart);
        box.getStyleClass().add("output_plot_background");

        this.newContent.add(box);
    }

    private void updateDataSeries(DataSeriesStateValue dataSeries, ObservableList<Series<Number, Number>> property) {
        Map<String, List<DataPoint>> newData = new HashMap<>();

        // executed by state for automatic handling of locking
        this.state.execute(s -> newData.putAll(dataSeries.getChangedData()));

        List<Series<Number, Number>> seriesToRemove = new ArrayList<>();

        for (Series<Number, Number> series : property) {
            List<DataPoint> data = newData.remove(series.getName());

            if (data == null && dataSeries.getSeriesSize(series.getName()) > 0) {
                continue;
            } else if (data == null || dataSeries.getSeriesSize(series.getName()) == 0) {
                seriesToRemove.add(series);
                continue;
            } else if (dataSeries.getSeriesSize(series.getName()) < series.getData().size()) {
                series.getData().clear();
            }

            for (DataPoint point : data) {
                if (point.getIndex() >= series.getData().size()) {
                    series.getData().add(new XYChart.Data<>(point.getX(), point.getY()));
                } else {
                    series.getData().get(point.getIndex()).setYValue(point.getY());
                }
            }
        }

        property.removeAll(seriesToRemove);

        for (Entry<String, List<DataPoint>> newSeries : newData.entrySet()) {
            ObservableList<XYChart.Data<Number, Number>> data = FXCollections.observableArrayList();

            newSeries.getValue()
                    .stream()
                    .map(p -> new XYChart.Data<Number, Number>(p.getX(), p.getY()))
                    .forEach(data::add);

            Series<Number, Number> series = new Series<>(newSeries.getKey(), data);
            property.add(series);
        }
    }

    // *********************************************
    // Methods for handling number data
    // *********************************************

    private void refreshFieldForNumber(StateParameter<?> stateType, NumberStateValue numberState, String suffix) {
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
        if (graphState.unchangedSinceLastGet() || graphState.getValue() == null || graphState.getValue().connections().isEmpty()) {
            return;
        }

        GraphStateValue.GraphData graphData = graphState.getValue();

        String mapIdentifier = createMapIdentifier(stateType, suffix + ".view");
        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createGraphView(stateType, suffix);
        }

        //noinspection unchecked if creation works properly issues with casting not possible
        GraphView<String, String> graphView = (GraphView<String, String>) this.visualRepresentations.get(mapIdentifier);
        //noinspection unchecked if creation works properly issues with casting not possible
        Graph<String, String> graph = (Graph<String, String>) this.visualRepresentations.get(createMapIdentifier(stateType, suffix + ".graph"));

        // completely reset the graph --> update would be possible but harder to implement and may not save time
        graph.edges().forEach(graph::removeEdge);
        graph.vertices().forEach(graph::removeVertex);

        Set<String> existingVertices = new HashSet<>();

        for (Connection connection : graphData.connections()) {
            if (existingVertices.add(connection.start())) {
                graph.insertVertex(connection.start());
            }
            if (existingVertices.add(connection.target())) {
                graph.insertVertex(connection.target());
            }
            String edgeLabel = String.format("%s->%s [%.3f]", connection.start(), connection.target(), connection.weight());
            Edge<String, String> edge = graph.insertEdge(connection.start(), connection.target(), edgeLabel);
            graphView.setStyle(edge, getEdgeStyle(connection.weight(), graphData.minWeight(), graphData.maxWeight()));
        }

        graphView.update();
    }

    private String getEdgeStyle(double weight, double lowerBound, double upperBound) {
        if (weight < 0) {
            weight /= Math.abs(lowerBound);
        } else {
            weight /= upperBound;
        }

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
        GraphView<String, Double> graphView = new GraphView<>(graph, properties, null, Objects.requireNonNull(this.getClass().getResource("/gui/graph/graph.css")).toExternalForm());

        this.visualRepresentations.put(createMapIdentifier(stateType, suffix + ".view"), graphView);
        this.visualRepresentations.put(createMapIdentifier(stateType, suffix + ".graph"), graph);
        this.newContent.add(createCard(stateType, graphView));

        graphView.setMinWidth(250);
        graphView.setMinHeight(250);

        graphView.init();
    }

    // *********************************************
    // Methods for handling distribution data
    // *********************************************

    private void refreshDistributionState(StateParameter<?> stateType, DistributionStateValue distributionState, String suffix) {
        if (distributionState.unchangedSinceLastGet()) {
            return;
        }

        String mapIdentifier = createMapIdentifier(stateType, suffix);
        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createFieldForDistribution(stateType, suffix);
        }

        Label label = (Label) this.visualRepresentations.get(mapIdentifier);
        DecimalFormat format = new DecimalFormat("#.#####");
        String mean = format.format(distributionState.getMean());
        String deviation = format.format(distributionState.getStandardDeviation());

        label.setText(String.format("Mean: %s - Deviation: %s", mean, deviation));
    }

    private void createFieldForDistribution(StateParameter<?> stateType, String suffix) {
        Label label = new Label();
        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), label);
        label.getStyleClass().add("number_field");

        this.newContent.add(createCard(stateType, label));
    }

    // *********************************************
    // Methods for handling scattered data
    // *********************************************
    
    private void refreshScatteredDataState(StateParameter<?> stateType, ScatteredDataStateValue scatteredValue, String suffix) {
        // Unused because of performance issues
//        if (!scatteredValue.changedSinceLastGet()) {
//            return;
//        }
//
//        String mapIdentifier = createMapIdentifier(stateType, suffix);
//        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
//            createScatterPlot(stateType, suffix);
//        }
//
//        //noinspection unchecked no safe way to cast generic --> if generation on top works correctly is always valid cast
//        ScatterChart<Number, Number> chart = (ScatterChart<Number, Number>) this.visualRepresentations.get(mapIdentifier);
//
//        updateDataSeries(scatteredValue, chart.getData(), stateType.getName());
    }

    private void createScatterPlot(StateParameter<?> stateType, String suffix) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Evaluation number");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fitness");
        ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

        chart.getStyleClass().add("output_plot");
        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), chart);

        VBox box = createCard(stateType, chart);
        box.getStyleClass().add("output_plot_background");

        this.newContent.add(box);
    }

    private void updateDataSeries(ScatteredDataStateValue scatteredData, ObservableList<Series<Number, Number>> property, String name) {
        Map<Integer, Double[]> seriesData = scatteredData.getValue();

        if (property.isEmpty()) {
            Series<Number, Number> series = new Series<>();
            series.setName(name);
            property.add(series);
        }

        if (seriesData.isEmpty()) {
            property.get(0).getData().clear();
            return;
        }

        Series<Number, Number> series = property.get(0);
        Set<Integer> keysToRefresh = scatteredData.getIndicesToRefresh();

        if (seriesData.size() < series.getData().size()) {
            // if only data for one iteration exists refresh the complete plot
            // happens after refresh --> FIXME may cause problems if after refresh more than one iteration is contained
            series.getData().clear();
            keysToRefresh.addAll(seriesData.keySet());
        }

        keysToRefresh.forEach(k -> addScatteredData(k, seriesData.get(k), series));

        // all indices were refreshed
        scatteredData.getIndicesToRefresh().clear();
    }

    private void addScatteredData(Integer yValue, Double[] xValues, Series<Number, Number> series) {
        Arrays.stream(xValues)
                .map(x -> new XYChart.Data<Number, Number>(x, yValue))
                .forEach(d -> series.getData().add(d));
    }

    // **************************************************
    // Methods for handling quantity values
    // **************************************************

    private void refreshPieChartForQuantity(StateParameter<?> stateType, DataQuantityStateValue quantityValue, String suffix) {
        String mapIdentifier = createMapIdentifier(stateType, suffix);

        if (!this.visualRepresentations.containsKey(mapIdentifier)) {
            createPieChart(quantityValue, stateType, suffix);
        }

        PieChart chart = (PieChart) this.visualRepresentations.get(mapIdentifier);
        updatePieChart(quantityValue, chart.getData());
    }

    private void createPieChart(DataQuantityStateValue quantityValue, StateParameter<?> stateType, String suffix) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        PieChart chart = new PieChart(pieChartData);
        chart.getStyleClass().add("output_plot");
        chart.setAnimated(false);

        this.visualRepresentations.put(createMapIdentifier(stateType, suffix), chart);

        VBox box = createCard(stateType, chart);
        box.getStyleClass().add("output_plot_background");

        this.newContent.add(box);
    }

    private void updatePieChart(DataQuantityStateValue quantityValue, ObservableList<PieChart.Data> property) {
        Map<String, Long> newData = new HashMap<>();

        // executed by state for automatic handling of locking
        this.state.execute(s -> newData.putAll(quantityValue.getValue()));

        List<PieChart.Data> dataToRemove = new ArrayList<>();

        for (PieChart.Data pieData : property) {
            Long data = newData.remove(pieData.getName());

            if (data == null) {
                dataToRemove.add(pieData);
            } else {
                pieData.setPieValue(data);
            }
        }

        property.removeAll(dataToRemove);

        newData.entrySet()
                .stream()
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .forEach(property::add);

    }

    // **************************************************
    // Methods for handling collection distribution value
    // **************************************************

    private void refreshCollectionDistributionState(StateParameter<?> stateType, CollectionDistributionStateValue collectionValue, String suffix) {
        refreshPropertyOfState(stateType, collectionValue.getAlternativeRepresentation(), suffix);
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
