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

        private NeuronID currentNeuron;
        private int currentConnectionIndex;

        private Connection next;

        public ConnectionIterator(NeuralNetwork nn) {
            this.nn = nn;
            this.currentNeuron = this.nn.getNeuronsOfLayer(0).get(0);
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
            if (this.currentConnectionIndex >= currentConnections.size()) {
                selectNextNeuron();
                // restart with next neuron
                return calculateNext();
            }

            NeuronID target = currentConnections.get(this.currentConnectionIndex);
            double weight = this.nn.getWeightOfConnection(this.currentNeuron, target);
            this.currentConnectionIndex++;

            return new Connection(this.currentNeuron, target, weight);
        }

        private void selectNextNeuron() {
            List<NeuronID> neuronsOfSameLayer = this.nn.getNeuronsOfLayer(this.currentNeuron.getLayerIndex());

            // check if all neurons of this layer were iterated
            if (this.currentNeuron.getNeuronIndex() == neuronsOfSameLayer.size() - 1) {
                // select first neuron of the next layer if neuron wasn't the last one
                this.currentNeuron = this.currentNeuron.getLayerIndex() < this.nn.getDepth() - 1
                        ? this.nn.getNeuronsOfLayer(this.currentNeuron.getLayerIndex() + 1).get(0)
                        : null;
            } else {
                // select next neuron of this layer
                this.currentNeuron = this.nn.getNeuronsOfLayer(this.currentNeuron.getLayerIndex())
                        .get(this.currentNeuron.getNeuronIndex() + 1);
            }
            this.currentConnectionIndex = 0;
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

}
