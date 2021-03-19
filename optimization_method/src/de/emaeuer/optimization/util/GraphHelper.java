package de.emaeuer.optimization.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.List;

public class GraphHelper {

    private GraphHelper() {}

    public static Graph<String, DefaultWeightedEdge> transformToAdjacencyList(NeuralNetwork nn) {
        Graph<String, DefaultWeightedEdge> graph = GraphTypeBuilder
                .<String, DefaultWeightedEdge>directed()
                .allowingMultipleEdges(false)
                .allowingSelfLoops(true)
                .edgeClass(DefaultWeightedEdge.class)
                .weighted(true)
                .buildGraph();

        for (int i = 0; i < nn.getDepth(); i++) {
            List<NeuronID> neuronsOfLayer = nn.getNeuronsOfLayer(i);
            for (NeuronID start : neuronsOfLayer) {
                List<NeuronID> targets = nn.getOutgoingConnectionsOfNeuron(start);
                targets.forEach(t -> Graphs.addEdge(graph, createOrFindVertex(start, graph), createOrFindVertex(t, graph), nn.getWeightOfConnection(start, t)));
            }
        }

        return graph;
    }

    private static String createOrFindVertex(NeuronID id, Graph<String, DefaultWeightedEdge> graph) {
        String vertex = id.getLayerIndex() + "-" + id.getNeuronIndex();

        if (!graph.containsVertex(vertex)) {
            graph.addVertex(vertex);
        }

        return vertex;
    }

}
