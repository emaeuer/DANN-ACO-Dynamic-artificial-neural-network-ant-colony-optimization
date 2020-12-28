package de.uni.ann;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NeuralNetworkLayer implements Iterable<Neuron> {

    private final RealMatrix weights;
    private final RealVector bias;

    private final NeuralNetworkLayer previousLayer;
    private NeuralNetworkLayer nextLayer;

    private final DoubleFunction<Double> activationFunction;

    private final List<Neuron> neurons = new ArrayList<>();

    /**
     * Constructor for input and hidden layers with default activation function {@link ActivationFunctions}.LINEAR_UNTIL_SATURATION
     *
     * @param numberOfNeurons Number of neurons in this layer
     * @param previousLayer   The layer from which this layer receives inputs
     */
    public NeuralNetworkLayer(int numberOfNeurons, NeuralNetworkLayer previousLayer, int layerId) {
        IntStream.range(0, numberOfNeurons)
                .forEach(i -> this.neurons.add(new Neuron(new Neuron.NeuronID(layerId, i), this)));

        this.weights = MatrixUtils.createRealMatrix(numberOfNeurons, previousLayer.getNumberOfNeurons());
        this.bias = new ArrayRealVector(numberOfNeurons);
        this.previousLayer = previousLayer;
        this.activationFunction = ActivationFunctions.LINEAR_UNTIL_SATURATION;

        this.previousLayer.nextLayer = this;
        this.previousLayer.initializeNeurons();
    }

    /**
     * Constructor for input layer
     *
     * @param numberOfNeurons Number of neurons in this layer
     */
    public NeuralNetworkLayer(int numberOfNeurons, int layerId) {
        IntStream.range(0, numberOfNeurons)
                .forEach(i -> this.neurons.add(new Neuron(new Neuron.NeuronID(layerId, i), this)));

        this.weights = null;
        this.bias = null;
        this.previousLayer = null;
        this.activationFunction = null;
    }

    private void initializeNeurons() {
        this.neurons.forEach(n -> n.initializeNeuron(this.nextLayer.weights, this.nextLayer.neurons));
    }

    public int getNumberOfNeurons() {
        return this.neurons.size();
    }

    public RealVector processInput(RealVector input) {
        RealVector output = input;
        if (!isInputLayer()) {
            output = this.weights.operate(input)
                    .add(this.bias)
                    .map(this.activationFunction::apply);
        }

        // pass output to next layer or return the output in the last layer
        return isOutputLayer() ? output : this.nextLayer.processInput(output);
    }

    public Neuron getNeuron(int id) {
        return this.neurons.get(id);
    }

    public boolean isInputLayer() {
        return this.weights == null && this.bias == null;
    }

    public boolean isOutputLayer() {
        return this.nextLayer == null;
    }

    public NeuralNetworkLayer getNextLayer() {
        return this.nextLayer;
    }

    public Stream<Neuron> stream() {
        return this.neurons.stream();
    }

    public void forEach(Consumer<? super Neuron> consumer) {
        this.neurons.forEach(consumer);
    }

    public RealMatrix getWeights() {
        return this.weights;
    }

    public void setWeights(RealMatrix weights) {
        // only changes the existing matrix to keep all references consistent
        for (int i = 0; i < weights.getRowDimension(); i++) {
            this.weights.setRowVector(i, weights.getRowVector(i));
        }
    }

    public RealVector getBias() {
        return this.bias;
    }

    public void setBias(RealVector bias) {
        // only changes the existing vector to keep all references consistent
        for (int i = 0; i < this.bias.getDimension(); i++) {
            this.bias.setEntry(i, bias.getEntry(i));
        }
    }

    @Override
    public Iterator<Neuron> iterator() {
        return this.neurons.iterator();
    }

    @Override
    public Spliterator<Neuron> spliterator() {
        return this.neurons.spliterator();
    }
}

