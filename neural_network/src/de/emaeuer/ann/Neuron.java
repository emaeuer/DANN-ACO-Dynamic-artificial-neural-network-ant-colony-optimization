package de.emaeuer.ann;

import de.emaeuer.ann.impl.NeuralNetworkImpl;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstraction for {@link NeuralNetworkImpl} and {@link NeuralNetworkLayerImpl} class. Doesn't have any own data just changes
 * the corresponding data in its neural network layer
 */
public class Neuron {

    public record NeuronID(int layerID, int neuronID) {}

    private int inLayerID;

    private final NeuralNetworkLayer containingLayer;

    private final ArrayList<Connection> outgoingConnections = new ArrayList<>();
    private final ArrayList<Connection> incomingConnections = new ArrayList<>();

    public Neuron(int inLayerID, NeuralNetworkLayer containingLayer) {
        this.containingLayer = containingLayer;
        this.inLayerID = inLayerID;
    }

    public void delete() {
        getContainingLayer().modify()
                .removeNeuron(this.inLayerID);
    }

    public void addConnectionFrom(Neuron start, double weight) {
        this.containingLayer.modify()
                .addConnection(start, this, weight);
    }

    public double getBias() {
        return this.containingLayer.getBiasOf(this.inLayerID);
    }

    public void setBias(double bias) {
        this.containingLayer.setBiasOf(this.inLayerID, bias);
    }

    public double getLastActivation() {
        return this.containingLayer.getActivationOf(this.inLayerID);
    }

    public NeuralNetworkLayer getContainingLayer() {
        return this.containingLayer;
    }

    public NeuronID getNeuronID() {
        return new NeuronID(getLayerID(), getNeuronInLayerID());
    }

    public int getNeuronInLayerID() {
        return this.inLayerID;
    }

    public void setInLayerID(int inLayerID) {
        this.inLayerID = inLayerID;
    }

    public int getLayerID() {
        return this.containingLayer.getLayerID();
    }

    public List<Connection> getOutgoingConnections() {
        return this.outgoingConnections;
    }

    public List<Connection> getIncomingConnections() {
        return this.incomingConnections;
    }

    public boolean hasConnectionTo(NeuralNetworkLayer neuralNetworkLayer) {
        return this.outgoingConnections.stream()
                .anyMatch(c -> c.end().getContainingLayer() == neuralNetworkLayer);
    }

    public boolean hasConnectionTo(Neuron end) {
        return this.outgoingConnections.stream()
                .anyMatch(c -> c.end().getNeuronID().equals(end.getNeuronID()));
    }

    public Connection getConnectionTo(Neuron end) {
        return this.outgoingConnections.stream()
                .filter(c -> c.end().getNeuronID().equals(end.getNeuronID()))
                .findFirst()
                .orElse(null);
    }
}
