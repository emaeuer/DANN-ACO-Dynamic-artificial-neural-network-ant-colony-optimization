package de.emaeuer.ann.util;

import de.emaeuer.ann.*;
import de.emaeuer.ann.Connection.ConnectionPrototype;
import de.emaeuer.ann.impl.NeuralNetworkImpl;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;
import de.emaeuer.ann.Neuron.NeuronID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NeuralNetworkLayerBuilder {

    private static final String EXCEPTION_MESSAGE_PATTERN = "Failed to create neural network layer because attribute \"%s\" was not set";

    private NeuralNetworkLayerImpl layer = new NeuralNetworkLayerImpl();

    private Map<NeuronID, List<ConnectionPrototype>> connections = new HashMap<>();

    private int necessaryModificationFlag = 0b1111;

    public NeuralNetworkLayerBuilder() {
        layer.setActivationFunction(ActivationFunctions.IDENTITY);
    }

    public NeuralNetworkLayerBuilder activationFunction(DoubleFunction<Double> function) {
        layer.setActivationFunction(function);
        return this;
    }

    public NeuralNetworkLayerBuilder numberOfNeurons(int number) {
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
                .forEach(i -> this.layer.getNeurons().add(new Neuron(i, this.layer)));

        return this;
    }

    public NeuralNetworkLayerBuilder layerType(NeuralNetworkLayer.LayerType type) {
        this.necessaryModificationFlag &= 0b1011;
        this.layer.setLayerType(type);
        return this;
    }

    public NeuralNetworkLayerBuilder neuralNetwork(NeuralNetwork neuralNetwork) {
        Objects.requireNonNull(neuralNetwork);
        this.necessaryModificationFlag &= 0b1101;
        this.layer.setNeuralNetwork((NeuralNetworkImpl) neuralNetwork);
        return this;
    }

    public NeuralNetworkLayerBuilder layerID(int id) {
        this.necessaryModificationFlag &= 0b1110;
        this.layer.setLayerID(id);
        return this;
    }

    public NeuralNetworkLayerBuilder addConnection(ConnectionPrototype... connections) {
        Arrays.stream(connections)
                .peek(c -> this.connections.putIfAbsent(c.startID(), new ArrayList<>()))
                .forEach(c -> this.connections.get(c.startID()).add(c));
        return this;
    }

    public NeuralNetworkLayerBuilder fullyConnectTo(NeuralNetworkLayer other) {
        for (Neuron neuron : other) {
            NeuronID id = neuron.getNeuronID();
            List<ConnectionPrototype> connections = this.layer.stream()
                    .map(end -> new ConnectionPrototype(id, end.getNeuronID()))
                    .collect(Collectors.toList());
            this.connections.computeIfAbsent(id, n -> new ArrayList<>());
            this.connections.get(id).addAll(connections);
        }

        return this;
    }

    public NeuralNetworkLayerBuilder bias(RealVector bias) {
        if (bias.getDimension() != this.layer.getNumberOfNeurons()) {
            throw new IllegalArgumentException("Invalid bias vector (dimension doesn't match number of neurons in this layer");
        }
        this.layer.setBias(bias);
        return this;
    }

    public NeuralNetworkLayer finish() {
        if (this.necessaryModificationFlag != 0) {
            throw new IllegalStateException(buildMessageForCurrentModificationFlag());
        }

        processConnections();

        // disable further modification
        NeuralNetworkLayer finishedLayer = this.layer;
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

        for (Entry<NeuronID, List<ConnectionPrototype>> entry : this.connections.entrySet()) {
            // add input neuron
            this.layer.getInputNeurons().add(this.layer.getNeuralNetwork().getNeuron(entry.getKey()));
            for (ConnectionPrototype prototype : entry.getValue()) {
                // set weight for connection in weight matrix
                this.layer.getWeights().setEntry(prototype.endID().neuronID(), this.layer.getInputNeurons().size() - 1, prototype.weight());
                createRealConnectionForPrototype(prototype);
            }
        }
    }

    private void createRealConnectionForPrototype(ConnectionPrototype prototype) {
        // create real connection for prototype and register the connection in the corresponding neurons
        Connection connection = new Connection(this.layer.getNeuralNetwork().getNeuron(prototype.startID()), this.layer.getNeuron(prototype.endID().neuronID()), this.layer);
        connection.start().getOutgoingConnections().add(connection);
        connection.end().getIncomingConnections().add(connection);
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
