package de.emaeuer.ann2;

/**
 * Abstraction for {@link NeuralNetwork} and {@link NeuralNetworkLayer} class. Doesn't have any own data just changes
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

    public Connection(Neuron start, Neuron end, NeuralNetworkLayer targetLayer) {
        this.start = start;
        this.end = end;
        this.targetLayer = targetLayer;

        start.addOutgoingConnection(this);
        end.addIncomingConnection(this);
    }

    public void delete() {
        this.start.removeOutgoingConnection(this);
        this.end.removeIncomingConnection(this);
    }

    public void splitConnection() {
        this.targetLayer.splitConnection(this);
    }

    public double getWeight() {
        return targetLayer.getWeightOf(this);
    }

    public void setWeight(double weight) {
         this.targetLayer.setWeightOf(this, weight);
    }
}
