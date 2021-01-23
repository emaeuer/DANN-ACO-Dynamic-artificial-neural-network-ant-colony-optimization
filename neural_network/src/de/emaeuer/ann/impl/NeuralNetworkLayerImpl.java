package de.emaeuer.ann.impl;

import de.emaeuer.ann.Connection;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.Neuron;
import de.emaeuer.ann.Neuron.NeuronID;
import de.emaeuer.ann.util.NeuralNetworkLayerModifier;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.stream.Stream;

public class NeuralNetworkLayerImpl implements NeuralNetworkLayer {

    private LayerType type;

    private int layerID;

    private DoubleFunction<Double> activationFunction;

    private RealMatrix weights;
    private RealVector bias;
    private RealVector activation = null;

    private final List<Neuron> neuronsOfLayer = new ArrayList<>();
    private final List<Neuron> inputNeurons = new ArrayList<>();

    private NeuralNetworkImpl neuralNetwork;

    private final NeuralNetworkLayerModifier modifier = new NeuralNetworkLayerModifier(this);

    public RealVector process(RealVector externalInput) {
        if (!isInputLayer()) {
            throw new IllegalArgumentException("Only the input layer can process an external input vector");
        }
        return processVector(externalInput);
    }

    @Override
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
                .mapToDouble(Neuron::getLastActivation)
                .toArray());
    }

    @Override
    public Iterator<Neuron> iterator() {
        return this.neuronsOfLayer.iterator();
    }

    @Override
    public void forEach(Consumer<? super Neuron> action) {
        this.neuronsOfLayer.forEach(action);
    }

    @Override
    public Spliterator<Neuron> spliterator() {
        return this.neuronsOfLayer.spliterator();
    }

    @Override
    public Stream<Neuron> stream() {
        return this.neuronsOfLayer.stream();
    }

    @Override
    public int getLayerID() {
        return this.layerID;
    }

    public void setLayerID(int id) {
        this.layerID = id;
    }

    @Override
    public int getNumberOfNeurons() {
        // use dimension of activation because it always equal to the number of neurons and is initialized before list of neurons
        return this.activation.getDimension();
    }

    @Override
    public Neuron getNeuron(int neuronIndex) {
        if (neuronIndex > getNumberOfNeurons()) {
            throw new IllegalArgumentException(String.format("Can't find neuron with neuronIndex = %d because the neural network layer only contains %d neurons", neuronIndex, getNumberOfNeurons()));
        }
        return this.neuronsOfLayer.get(neuronIndex);
    }

    @Override
    public boolean isInputLayer() {
        return this.type == LayerType.INPUT;
    }

    @Override
    public boolean isOutputLayer() {
        return this.type == LayerType.OUTPUT;
    }

    public double getActivationOf(int inLayerID) {
        return this.activation.getEntry(inLayerID);
    }

    @Override
    public RealVector getBias() {
        return this.bias;
    }

    @Override
    public RealVector getActivation() {
        return this.activation;
    }

    @Override
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

    @Override
    public double getWeightOf(NeuronID start, NeuronID end) {
        if (isInputLayer()) {
            throw new IllegalStateException("Connections to the input layer have no weight");
        } else if (end.layerID() != this.layerID) {
            throw new IllegalArgumentException(String.format("Can't retrieve weight of the connection from %s to %s in layer %d", start, end, this.layerID));
        }

        int indexOfInput = this.inputNeurons.indexOf(this.neuralNetwork.getNeuron(start));

        if (indexOfInput == -1) {
            throw new IllegalArgumentException(String.format("The connection from %s to %s doesn't exist", start, end));
        }

        return this.weights.getEntry(end.neuronID(), indexOfInput);
    }

    public void setWeightOf(Connection connection, double weight) {
        setWeightOf(connection.start().getNeuronID(), connection.end().getNeuronID(), weight);
    }

    @Override
    public void setWeightOf(NeuronID start, NeuronID end, double weight) {
        if (isInputLayer()) {
            throw new IllegalStateException("Can't change weight of connection to the input layer");
        } else if (end.layerID() != this.layerID) {
            throw new IllegalArgumentException(String.format("Can't change weight of the connection from %s to %s in layer %d", start, end, this.layerID));
        }

        int indexOfInput = this.inputNeurons.indexOf(this.neuralNetwork.getNeuron(start));

        if (indexOfInput == -1) {
            throw new IllegalArgumentException(String.format("The connection from %s to %s doesn't exist", start, end));
        }

        this.weights.setEntry(end.neuronID(), indexOfInput, weight);
    }

    @Override
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

    public NeuralNetwork getNeuralNetwork() {
        return this.neuralNetwork;
    }

    public void setNeuralNetwork(NeuralNetworkImpl neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }

    public void setWeights(RealMatrix weights) {
        this.weights = weights;
    }

    @Override
    public List<Neuron> getNeurons() {
        return this.neuronsOfLayer;
    }

    @Override
    public LayerType getType() {
        return this.type;
    }

    public List<Neuron> getInputNeurons() {
        return this.inputNeurons;
    }

}
