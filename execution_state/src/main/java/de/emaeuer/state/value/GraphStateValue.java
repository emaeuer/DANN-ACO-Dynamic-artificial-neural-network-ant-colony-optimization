package de.emaeuer.state.value;

import java.util.List;
import java.util.stream.Collectors;

public class GraphStateValue extends AbstractStateValue<GraphStateValue.GraphData, GraphStateValue.GraphData> {

    public static record GraphData(List<Connection> connections, double maxWeight, double minWeight) {}

    public static record Connection(String start, double weight, String target) {}

    private GraphData data;

    @Override
    public Class<? extends GraphData> getExpectedInputType() {
        return GraphData.class;
    }

    @Override
    public Class<? extends GraphData> getOutputType() {
        return GraphData.class;
    }

    @Override
    protected String handleNewValue(GraphData value) {
        this.data = value;
        return null;
    }

    @Override
    public GraphData getValueImpl() {
        return this.data;
    }

    @Override
    public String getExportValue() {
        return data.connections.stream()
                .map(c -> String.format("[%s-{%f}->%s]", c.start(), c.weight(), c.target()))
                .collect(Collectors.joining(""));
    }
}
