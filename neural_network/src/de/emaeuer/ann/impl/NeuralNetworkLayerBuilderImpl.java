package de.emaeuer.ann.impl;

import de.emaeuer.ann.*;
import de.emaeuer.ann.NeuronID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;

public class NeuralNetworkLayerBuilderImpl implements NeuralNetworkLayerBuilder {

    private static final String EXCEPTION_MESSAGE_PATTERN = "Failed to create neural network layer because attribute \"%s\" was not set";

    private NeuralNetworkLayerImpl layer = new NeuralNetworkLayerImpl();

    private Map<NeuronID, Map<NeuronID, Double>> connections = new HashMap<>();

    private int necessaryModificationFlag = 0b1111;

    public NeuralNetworkLayerBuilderImpl() {
        layer.setActivationFunction(ActivationFunction.IDENTITY);
    }

    @Override
    public NeuralNetworkLayerBuilderImpl activationFunction(ActivationFunction function) {
        layer.setActivationFunction(function);
        return this;
    }

    @Override
    public NeuralNetworkLayerBuilderImpl numberOfNeurons(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("The layer must contain at least one neuron");
        }

        this.necessaryModificationFlag &= 0b0111;
        // don't initialize weights of layer here because the connections and type of the layer can still change
        this.layer.setBias(new ArrayRealVector(number));
        // the initial activation of each neuron is 0
        this.layer.setActivation(new ArrayRealVector(number));

        // initialize neurons
        this.layer.getNeurons().clear();
        IntStream.range(0, this.layer.getNumberOfNeurons())
                .forEach(i -> this.layer.getNeurons().add(new NeuronID(this.layer.getLayerIndex(), i)));

        return this;
    }

    public NeuralNetworkLayerBuilderImpl layerType(LayerType type) {
        this.necessaryModificationFlag &= 0b1011;
        this.layer.setLayerType(type);
        return this;
    }

    public NeuralNetworkLayerBuilderImpl neuralNetwork(NeuralNetwork neuralNetwork) {
        Objects.requireNonNull(neuralNetwork);
        this.necessaryModificationFlag &= 0b1101;
        this.layer.setNeuralNetwork((NeuralNetworkImpl) neuralNetwork);
        return this;
    }

    public NeuralNetworkLayerBuilderImpl layerID(int id) {
        this.necessaryModificationFlag &= 0b1110;
        this.layer.setLayerIndex(id);
        this.layer.getNeurons().forEach(n -> n.setLayerIndex(id));
        return this;
    }

    @Override
    public NeuralNetworkLayerBuilderImpl addConnection(NeuronID start, NeuronID end, double weight) {
        end = this.layer.getNeurons().get(end.getNeuronIndex());

        this.connections.putIfAbsent(start, new HashMap<>());
        this.connections.get(start).put(end, weight);

        return this;
    }

    @Override
    public NeuralNetworkLayerBuilderImpl fullyConnectTo(List<NeuronID> otherNeurons) {
        for (NeuronID neuron : otherNeurons) {
            this.layer.getNeurons().forEach(n -> addConnection(neuron, n, 0));
        }

        return this;
    }

    @Override
    public NeuralNetworkLayerBuilderImpl bias(RealVector bias) {
        if (bias.getDimension() != this.layer.getNumberOfNeurons()) {
            throw new IllegalArgumentException("Invalid bias vector (dimension doesn't match number of neurons in this layer");
        }
        this.layer.setBias(bias);
        return this;
    }

    @Override
    public NeuralNetworkLayerBuilderImpl maxWeight(double value) {
        this.layer.setMaxWeight(value);
        return this;
    }

    @Override
    public NeuralNetworkLayerBuilderImpl minWeight(double value) {
        this.layer.setMinWeight(value);
        return this;
    }

    public NeuralNetworkLayerImpl finish() {
        if (this.necessaryModificationFlag != 0) {
            throw new IllegalStateException(buildMessageForCurrentModificationFlag());
        }

        processConnections();

        // disable further modification
        NeuralNetworkLayerImpl finishedLayer = this.layer;
        this.layer = null;
        this.connections = null;
        return finishedLayer;
    }

    /**
     * Creates the weight matrix for the layer
     */
    private void processConnections() {
        if (this.layer.isInputLayer()) {
            this.layer.setWeights(null);
            this.layer.setActivationFunction(null);
            this.layer.setBias(null);
            this.connections.clear();
            return;
        } else {
            this.layer.setWeights(MatrixUtils.createRealMatrix(this.layer.getNumberOfNeurons(), this.connections.size()));
        }

        for (Entry<NeuronID, Map<NeuronID, Double>> entry : this.connections.entrySet()) {
            // add input neuron
            NeuronID start = entry.getKey();
            this.layer.getInputNeurons().add(start);
            for (Entry<NeuronID, Double> connection : entry.getValue().entrySet()) {
                NeuronID end = connection.getKey();
                double weight = connection.getValue();

                // set weight for connection in weight matrix
                this.layer.getWeights().setEntry(end.getNeuronIndex(), this.layer.getInputNeurons().indexOf(start), weight);

                // register connection in corresponding layers
                this.layer.getIncomingConnections().putIfAbsent(end, new ArrayList<>());
                this.layer.getIncomingConnections().get(end).add(start);
                NeuralNetworkLayerImpl startLayer = this.layer.getNeuralNetwork().getLayer(start.getLayerIndex());
                startLayer.getOutgoingConnections().putIfAbsent(start, new ArrayList<>());
                startLayer.getOutgoingConnections().get(start).add(end);
            }
        }
    }

    private String buildMessageForCurrentModificationFlag() {
        int firstMissingArgument = Integer.highestOneBit(this.necessaryModificationFlag);

        return switch (firstMissingArgument) {
            case 0 -> String.format(EXCEPTION_MESSAGE_PATTERN, "LayerID");
            case 1 -> String.format(EXCEPTION_MESSAGE_PATTERN, "NeuralNetwork");
            case 2 -> String.format(EXCEPTION_MESSAGE_PATTERN, "LayerType");
            case 3 -> String.format(EXCEPTION_MESSAGE_PATTERN, "NumberOfNeurons");
            default -> throw new IllegalStateException("Unexpected value: " + firstMissingArgument);
        };
    }
}
