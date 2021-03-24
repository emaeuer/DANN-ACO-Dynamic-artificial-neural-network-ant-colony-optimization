package de.emaeuer.optimization.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.state.value.GraphStateValue;
import de.emaeuer.state.value.GraphStateValue.Connection;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphHelper {

    private static final String NODE_LABEL_PATTERN = "%d-%d";

    private GraphHelper() {}

    /**
     * Just use list of all connections instead of specialized data structures like adjacency list because the graph is
     * small anyways and only refreshed if a new best solution was found. Also the visualization is easier for the list
     */
    public static List<Connection> transformToConnectionList(NeuralNetwork nn) {
        if (nn == null) {
            return Collections.emptyList();
        }

        List<Connection> connections = new ArrayList<>();

        for (int i = 0; i < nn.getDepth(); i++) {
            List<NeuronID> neuronsOfLayer = nn.getNeuronsOfLayer(i);
            for (NeuronID start : neuronsOfLayer) {
                String startName =getNodeLabel(start);
                nn.getOutgoingConnectionsOfNeuron(start)
                        .stream()
                        .map(t -> new Connection(startName, nn.getWeightOfConnection(start, t), getNodeLabel(t)))
                        .forEach(connections::add);
            }
        }

        return connections;
    }

    private static String getNodeLabel(NeuronID neuron) {
        return String.format(NODE_LABEL_PATTERN, neuron.getLayerIndex(), neuron.getNeuronIndex());
    }

}
