package de.emaeuer.ann;

import de.emaeuer.ann.impl.NeuralNetworkImpl;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;

/**
 * Abstraction for {@link NeuralNetworkImpl} and {@link NeuralNetworkLayerImpl} class. Doesn't have any own data just changes
 * the corresponding data in its neural network layer
 */
public record Connection(Neuron start, Neuron end, NeuralNetworkLayer targetLayer) {

    public record ConnectionPrototype(Neuron.NeuronID startID, Neuron.NeuronID endID, double weight) {

        public ConnectionPrototype(Neuron.NeuronID startID, Neuron.NeuronID endID, double weight) {
            this.startID = startID;
            this.endID = endID;
            this.weight = weight;
        }

        public ConnectionPrototype(Neuron.NeuronID startID, Neuron.NeuronID endID) {
            this(startID, endID, 0);
        }
    }

    public Connection(Neuron start, Neuron end) {
        this(start, end, end.getContainingLayer());
    }

    public void delete() {
        end().getContainingLayer()
                .modify()
                .removeConnection(this);
    }

    public void splitConnection() {
        this.targetLayer.getNeuralNetwork()
                .modify()
                .splitConnection(this);
    }

    public double getWeight() {
        return targetLayer.getWeightOf(start().getNeuronID(), end().getNeuronID());
    }

    public void setWeight(double weight) {
         this.targetLayer.setWeightOf(start().getNeuronID(), end().getNeuronID(), weight);
    }
}
