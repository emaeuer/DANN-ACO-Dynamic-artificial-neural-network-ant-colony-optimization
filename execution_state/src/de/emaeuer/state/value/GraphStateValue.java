package de.emaeuer.state.value;

import java.util.List;

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
    protected void handleNewValue(GraphData value) {
        this.data = value;
    }

    @Override
    public GraphData getValueImpl() {
        return this.data;
    }

}
