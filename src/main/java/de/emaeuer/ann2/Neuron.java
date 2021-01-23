package de.emaeuer.ann2;

import java.util.ArrayList;

/**
 * Abstraction for {@link NeuralNetwork} and {@link NeuralNetworkLayer} class. Doesn't have any own data just changes
 * the corresponding data in its neural network layer
 */
public class Neuron {

    public record NeuronID(int layerID, int neuronID) {}

    private int inLayerID;

    private final NeuralNetworkLayer containingLayer;

    private final ArrayList<Connection> outgoingConnections = new ArrayList<>();
    private final ArrayList<Connection> incomingConnections = new ArrayList<>();

    private boolean deletionInProcess = false; // used to prevent multiple deletions of this connections

    public Neuron(int inLayerID, NeuralNetworkLayer containingLayer) {
        this.containingLayer = containingLayer;
        this.inLayerID = inLayerID;
    }

    public void delete() {
        deletionInProcess = true;
        this.outgoingConnections.forEach(Connection::delete);
        this.incomingConnections.forEach(Connection::delete);
        this.containingLayer.remove(this);
    }

    public void addConnectionFrom(Neuron start, double weight) {
        this.containingLayer.addNewConnection(start, this, weight);
    }

    public double getBias() {
        return this.containingLayer.getBiasOf(this.inLayerID);
    }

    public void setBias(double bias) {
        this.containingLayer.setBiasOf(this.inLayerID, bias);
    }

    public double getLastActivation() {
        return this.containingLayer.getLastActivationOf(this.inLayerID);
    }

    public NeuralNetworkLayer getContainingLayer() {
        return this.containingLayer;
    }

    public NeuronID getNeuronID() {
        return new NeuronID(getLayerID(), getInLayerID());
    }

    public int getInLayerID() {
        return this.inLayerID;
    }

    public int getLayerID() {
        return this.containingLayer.getLayerID();
    }

    void setInLayerID(int inLayerID) {
        this.inLayerID = inLayerID;
    }

    public void addOutgoingConnection(Connection connection) {
        this.outgoingConnections.add(connection);
    }

    public void removeOutgoingConnection(Connection connection) {
        if (!deletionInProcess) {
            this.outgoingConnections.remove(connection);
        }
    }

    public void addIncomingConnection(Connection connection) {
        this.incomingConnections.add(connection);
    }

    public void removeIncomingConnection(Connection connection) {
        if (!deletionInProcess) {
            this.incomingConnections.remove(connection);
            this.containingLayer.remove(connection);
        }
    }

    public boolean hasConnectionTo(NeuralNetworkLayer neuralNetworkLayer) {
        return this.outgoingConnections.stream()
                .anyMatch(c -> c.end().getContainingLayer() == neuralNetworkLayer);
    }
}
