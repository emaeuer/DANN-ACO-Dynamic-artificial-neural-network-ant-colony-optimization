package de.emaeuer.state.value;

import de.emaeuer.state.StateHandler;
import org.apache.commons.math3.linear.RealMatrix;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphStateValue extends AbstractStateValue<Graph<String, DefaultWeightedEdge>, Graph<String, DefaultWeightedEdge>> {

    private Graph<String, DefaultWeightedEdge> graph;

    @Override
    public Class<? extends Graph<String, DefaultWeightedEdge>> getExpectedInputType() {
        Class<?> type = Graph.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Graph<String, DefaultWeightedEdge>>) type;
    }

    @Override
    public Class<? extends Graph<String, DefaultWeightedEdge>> getOutputType() {
        Class<?> type = Graph.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Graph<String, DefaultWeightedEdge>>) type;
    }

    @Override
    protected void handleNewValue(Graph<String, DefaultWeightedEdge> value) {
        this.graph = value;
    }

    @Override
    public Graph<String, DefaultWeightedEdge> getValueImpl() {
        return this.graph;
    }

}
