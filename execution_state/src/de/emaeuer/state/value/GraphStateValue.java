package de.emaeuer.state.value;

import java.util.List;

public class GraphStateValue extends AbstractStateValue<List<GraphStateValue.Connection>, List<GraphStateValue.Connection>> {

    public static record Connection(String start, double weight, String target) {}

    private List<Connection> connections;

    @Override
    public Class<? extends List<Connection>> getExpectedInputType() {
        Class<?> type = List.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends List<Connection>>) type;
    }

    @Override
    public Class<? extends List<Connection>> getOutputType() {
        Class<?> type = List.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends List<Connection>>) type;
    }

    @Override
    protected void handleNewValue(List<Connection> value) {
        this.connections = value;
    }

    @Override
    public List<Connection> getValueImpl() {
        return this.connections;
    }

}
