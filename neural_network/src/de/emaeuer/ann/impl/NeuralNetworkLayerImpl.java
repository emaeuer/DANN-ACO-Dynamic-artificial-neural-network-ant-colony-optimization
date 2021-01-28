package de.emaeuer.ann.impl;

import de.emaeuer.ann.LayerType;
import de.emaeuer.ann.NeuronID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.function.DoubleFunction;

public class NeuralNetworkLayerImpl {

    private LayerType type;

    private int layerIndex;

    private DoubleFunction<Double> activationFunction;

    private RealMatrix weights;
    private RealVector bias;
    private RealVector activation = null;

    private final List<NeuronID> neuronsOfLayer = new ArrayList<>();
    private final List<NeuronID> inputNeurons = new ArrayList<>();

    private NeuralNetworkImpl neuralNetwork;

    private final Map<NeuronID, List<NeuronID>> incomingConnections = new HashMap<>();
    private final Map<NeuronID, List<NeuronID>> outgoingConnections = new HashMap<>();

    private final NeuralNetworkLayerModifier modifier = new NeuralNetworkLayerModifier(this);

    public static NeuralNetworkLayerBuilderImpl build() {
        return new NeuralNetworkLayerBuilderImpl();
    }

    public RealVector process(RealVector externalInput) {
        if (!isInputLayer()) {
            throw new IllegalArgumentException("Only the input layer can process an external input vector");
        }
        return processVector(externalInput);
    }

    public RealVector process() {
        if (isInputLayer()) {
            throw new IllegalArgumentException("The input layer needs an input vector to process");
        }
        return processVector(buildInputVector());
    }

    private RealVector processVector(RealVector vector) {
        // the activation of the input layer is the external input vector
        RealVector output = switch (type) {
            case INPUT -> vector;
            case OUTPUT, HIDDEN -> this.weights.operate(vector)
                    .add(this.bias)
                    .map(this.activationFunction::apply);
        };

        this.activation = output;
        return output;
    }

    private RealVector buildInputVector() {
        return new ArrayRealVector(this.inputNeurons.stream()
                .mapToDouble(this.neuralNetwork::getLastActivationOf)
                .toArray());
    }

    public int getLayerIndex() {
        return this.layerIndex;
    }

    public void setLayerIndex(int id) {
        this.layerIndex = id;

        // refresh neuron layer index and corresponding map entries
        this.neuronsOfLayer.forEach(n -> modify().applyNeuronIDChange(n, new NeuronID(id, n.getNeuronIndex())));
    }

    public int getNumberOfNeurons() {
        // use dimension of activation because it always equal to the number of neurons and is initialized before list of neurons
        return this.activation.getDimension();
    }

    public boolean isInputLayer() {
        return this.type == LayerType.INPUT;
    }

    public boolean isOutputLayer() {
        return this.type == LayerType.OUTPUT;
    }

    public double getActivationOf(int inLayerID) {
        return this.activation.getEntry(inLayerID);
    }

    public RealVector getBias() {
        return this.bias;
    }

    public RealVector getActivation() {
        return this.activation;
    }

    public RealMatrix getWeights() {
        return this.weights;
    }

    public double getBiasOf(int inLayerID) {
        if (isInputLayer()) {
            throw new IllegalStateException("Neurons of the input layer have no bias");
        }
        return this.bias.getEntry(inLayerID);
    }

    public void setBiasOf(int inLayerID, double bias) {
        if (isInputLayer()) {
            throw new IllegalStateException("Can't change bias of input layer neuron");
        }
        this.bias.setEntry(inLayerID, bias);
    }

    public double getWeightOf(NeuronID start, NeuronID end) {
        if (isInputLayer()) {
            throw new IllegalStateException("Connections to the input layer have no weight");
        } else if (end.getLayerIndex() != this.layerIndex) {
            throw new IllegalArgumentException(String.format("Can't retrieve weight of the connection from %s to %s in layer %d", start, end, this.layerIndex));
        }

        int indexOfInput = this.inputNeurons.indexOf(start);

        if (indexOfInput == -1) {
            throw new IllegalArgumentException(String.format("The connection from %s to %s doesn't exist", start, end));
        }

        return this.weights.getEntry(end.getNeuronIndex(), indexOfInput);
    }

    public void setWeightOf(NeuronID start, NeuronID end, double weight) {
        if (isInputLayer()) {
            throw new IllegalStateException("Can't change weight of connection to the input layer");
        } else if (end.getLayerIndex() != this.layerIndex) {
            throw new IllegalArgumentException(String.format("Can't change weight of the connection from %s to %s in layer %d", start, end, this.layerIndex));
        }

        int indexOfInput = this.inputNeurons.indexOf(start);

        if (indexOfInput == -1) {
            throw new IllegalArgumentException(String.format("The connection from %s to %s doesn't exist", start, end));
        }

        this.weights.setEntry(end.getNeuronIndex(), indexOfInput, weight);
    }

    public NeuralNetworkLayerModifier modify() {
        return this.modifier;
    }

    public DoubleFunction<Double> getActivationFunction() {
        return this.activationFunction;
    }

    public void setActivationFunction(DoubleFunction<Double> activationFunction) {
        this.activationFunction = activationFunction;
    }

    public void setBias(RealVector bias) {
        this.bias = bias;
    }

    public void setActivation(RealVector activation) {
        this.activation = activation;
    }

    public void setLayerType(LayerType type) {
        this.type = type;
    }

    public NeuralNetworkImpl getNeuralNetwork() {
        return this.neuralNetwork;
    }

    public List<NeuronID> getOutgoingConnectionsOfNeuron(NeuronID neuron) {
        return this.outgoingConnections.getOrDefault(neuron, Collections.emptyList());
    }

    public Map<NeuronID, List<NeuronID>> getOutgoingConnections() {
        return this.outgoingConnections;
    }

    public List<NeuronID> getIncomingConnectionsOfNeuron(NeuronID neuron) {
        return this.incomingConnections.getOrDefault(neuron, Collections.emptyList());
    }

    public Map<NeuronID, List<NeuronID>> getIncomingConnections() {
        return this.incomingConnections;
    }

    public void setNeuralNetwork(NeuralNetworkImpl neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }

    public void setWeights(RealMatrix weights) {
        this.weights = weights;
    }

    public List<NeuronID> getNeurons() {
        return this.neuronsOfLayer;
    }

    public LayerType getType() {
        return this.type;
    }

    public List<NeuronID> getInputNeurons() {
        return this.inputNeurons;
    }

    public void addOutgoingConnection(NeuronID start, NeuronID end) {
        if (start.getLayerIndex() != this.layerIndex) {
            throw new UnsupportedOperationException(String.format("Can't add outgoing connection from %s to %s to layer %d", start, end, this.layerIndex));
        }

        this.getOutgoingConnections().putIfAbsent(start, new ArrayList<>());
        this.getOutgoingConnectionsOfNeuron(start).add(end);
    }

    public void addIncomingConnection(NeuronID start, NeuronID end) {
        if (end.getLayerIndex() != this.layerIndex) {
            throw new UnsupportedOperationException(String.format("Can't add incoming connection from %s to %s to layer %d", start, end, this.layerIndex));
        }

        this.getIncomingConnections().putIfAbsent(end, new ArrayList<>());
        this.getIncomingConnectionsOfNeuron(end).add(start);
    }
}
