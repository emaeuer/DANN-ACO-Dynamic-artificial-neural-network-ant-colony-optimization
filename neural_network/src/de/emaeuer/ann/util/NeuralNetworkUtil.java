package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;

import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class NeuralNetworkUtil {

    public static record Connection(NeuronID start, NeuronID end, double weight) {}

    private static class ConnectionIterator implements Iterator<Connection> {

        private final NeuralNetwork nn;
        private final Iterator<NeuronID> neuronIterator;

        private NeuronID currentNeuron;

        private int currentConnectionIndex;

        private Connection next;

        public ConnectionIterator(NeuralNetwork nn) {
            this.nn = nn;
            this.neuronIterator = NeuralNetworkUtil.iterateNeurons(this.nn);
            this.currentNeuron = neuronIterator.hasNext() ? this.neuronIterator.next() : null;
            this.next = calculateNext();
        }

        @Override
        public boolean hasNext() {
            // has next if end is not reached (last connection of last neuron of last layer)
            return this.next != null;
        }

        @Override
        public Connection next() {
            // return next and always calculate new next afterwards
            try {
                return this.next;
            } finally {
                this.next = calculateNext();
            }
        }

        private Connection calculateNext() {
            if (this.currentNeuron == null) {
                return null;
            }

            List<NeuronID> currentConnections = this.nn.getOutgoingConnectionsOfNeuron(this.currentNeuron);

            // if all connections of current neuron were iterated select next neuron
            while (this.currentConnectionIndex >= currentConnections.size()) {
                if (!this.neuronIterator.hasNext()) {
                    return null;
                }
                this.currentNeuron = this.neuronIterator.next();
                currentConnections = this.nn.getOutgoingConnectionsOfNeuron(this.currentNeuron);
                this.currentConnectionIndex = 0;
            }

            NeuronID target = currentConnections.get(this.currentConnectionIndex);
            double weight = this.nn.getWeightOfConnection(this.currentNeuron, target);
            this.currentConnectionIndex++;

            return new Connection(this.currentNeuron, target, weight);
        }
    }

    private NeuralNetworkUtil() {}

    public static Iterator<Connection> iterateNeuralNetworkConnections(NeuralNetwork nn) {
        return new ConnectionIterator(nn);
    }

    public static Iterator<NeuronID> iterateNeurons(NeuralNetwork nn) {
        return IntStream.range(0, nn.getDepth())
                .mapToObj(nn::getNeuronsOfLayer)
                .flatMap(List::stream)
                .iterator();
    }

    public static int countHiddenNodes(NeuralNetwork neuralNetwork) {
        // don't count neurons of input layer (i = 0) and output layer (i = depth - 1)
        return IntStream.range(1, neuralNetwork.getDepth() - 1)
                .mapToObj(neuralNetwork::getNeuronsOfLayer)
                .mapToInt(List::size)
                .sum();
    }

    public static int countConnections(NeuralNetwork neuralNetwork) {
        // iterate over all neurons and count their outgoing connections
        return IntStream.range(0, neuralNetwork.getDepth())
                .mapToObj(neuralNetwork::getNeuronsOfLayer)
                .flatMap(List::stream)
                .map(neuralNetwork::getOutgoingConnectionsOfNeuron)
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Builds a string that contains all connections
     *
     * @return String that contains all connections which can be used to identify equal prototypes
     */
    public static String getTopologySummary(NeuralNetwork nn) {
        StringBuilder builder = new StringBuilder();
        NeuralNetworkUtil.iterateNeuralNetworkConnections(nn)
                .forEachRemaining(c -> builder.append(String.format("[%d-%d->%d-%d]",
                        c.start().getLayerIndex(), c.start().getNeuronIndex(), c.end().getLayerIndex(), c.end().getNeuronIndex())));
        return builder.toString();
    }

    public static boolean isSmaller(NeuralNetwork nn1, NeuralNetwork nn2) {
        if (nn1 == null || nn2 == null) {
            throw new NullPointerException("Can't compare null neural network");
        }

        int hiddenNodes1 = NeuralNetworkUtil.countHiddenNodes(nn1);
        int hiddenNodes2 = NeuralNetworkUtil.countHiddenNodes(nn2);

        if (hiddenNodes1 < hiddenNodes2) {
            return true;
        } else if (hiddenNodes1 > hiddenNodes2) {
            return false;
        }

        int connections1 = NeuralNetworkUtil.countConnections(nn1);
        int connections2 = NeuralNetworkUtil.countConnections(nn2);

        return connections1 < connections2;
    }

}
