package de.emaeuer.ann2;

import de.emaeuer.math.MathUtil;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NeuralNetworkLayer implements Iterable<Neuron> {

    public static class NeuralNetworkLayerBuilder {
        private static final String EXCEPTION_MESSAGE_PATTERN = "Failed to create neural network layer because attribute \"%s\" was not set";

        private NeuralNetworkLayer layer = new NeuralNetworkLayer();

        private Map<Neuron.NeuronID, List<Connection.ConnectionPrototype>> connections = new HashMap<>();

        private int necessaryModificationFlag = 0b1111;

        private NeuralNetworkLayerBuilder() {
            layer.activationFunction = ActivationFunctions.LINEAR_UNTIL_SATURATION;
        }

        public NeuralNetworkLayerBuilder numberOfNeurons(int number) {
            if (number <= 0) {
                throw new IllegalArgumentException("The layer must contain at least one neuron");
            }

            this.necessaryModificationFlag &= 0b0111;
            // don't initialize weights of layer here because the connections and type of the layer can still change
            this.layer.bias = new ArrayRealVector(number);
            // the initial activation of each neuron is 0
            this.layer.activation = new ArrayRealVector(number);
            return this;
        }

        public NeuralNetworkLayerBuilder layerType(LayerType type) {
            this.necessaryModificationFlag &= 0b1011;
            this.layer.type = type;
            return this;
        }

        public NeuralNetworkLayerBuilder neuralNetwork(NeuralNetwork neuralNetwork) {
            Objects.requireNonNull(neuralNetwork);
            this.necessaryModificationFlag &= 0b1101;
            this.layer.neuralNetwork = neuralNetwork;
            return this;
        }

        public NeuralNetworkLayerBuilder layerID(int id) {
            this.necessaryModificationFlag &= 0b1110;
            this.layer.layerID = id;
            return this;
        }

        public NeuralNetworkLayerBuilder addConnection(Connection.ConnectionPrototype... connections) {
            Arrays.stream(connections)
                    .peek(c -> this.connections.putIfAbsent(c.startID(), new ArrayList<>()))
                    .forEach(c -> this.connections.get(c.startID()).add(c));
            return this;
        }

        public NeuralNetworkLayerBuilder fullyConnectTo(NeuralNetworkLayer other) {
            for (Neuron neuron: other) {
                Neuron.NeuronID id = neuron.getNeuronID();
                List<Connection.ConnectionPrototype> connections = this.layer.neuronsOfLayer.stream()
                        .map(end -> new Connection.ConnectionPrototype(id, end.getNeuronID()))
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
            this.layer.bias = bias;
            return this;
        }

        public NeuralNetworkLayer finish() {
            if (this.necessaryModificationFlag != 0) {
                throw new IllegalStateException(buildMessageForCurrentModificationFlag());
            }

            processConnections();

            initializeNeuronsAndIncomingConnections();

            // disable further modification
            NeuralNetworkLayer finishedLayer = this.layer;
            this.layer = null;
            this.connections = null;
            return finishedLayer;
        }

        private void processConnections() {
            if (this.layer.isInputLayer()) {
                this.layer.weights = null;
                this.layer.activationFunction = null;
                this.layer.bias = null;
                this.connections.clear();
                return;
            } else {
                this.layer.weights = MatrixUtils.createRealMatrix(this.layer.getNumberOfNeurons(), this.connections.size());
            }

            int currentNeuronIdInWeights = 0;

            for (Map.Entry<Neuron.NeuronID, List<Connection.ConnectionPrototype>> entry : this.connections.entrySet()) {
                this.layer.inputNeurons.add(this.layer.neuralNetwork.getNeuron(entry.getKey()));
                for (Connection.ConnectionPrototype connection : entry.getValue()) {
                    this.layer.weights.setEntry(connection.endID().layerID(), currentNeuronIdInWeights, connection.weight());
                }
                currentNeuronIdInWeights++;
            }
        }

        private void initializeNeuronsAndIncomingConnections() {
            // create neurons
            IntStream.range(0, this.layer.getNumberOfNeurons())
                    .forEach(i -> this.layer.neuronsOfLayer.add(new Neuron(i, this.layer)));

            // create real connections (this.connections are only dummies without function)
            this.connections.values()
                    .stream()
                    .flatMap(List::stream)
                    .forEach(c -> new Connection(this.layer.neuralNetwork.getNeuron(c.startID()),
                            this.layer.neuralNetwork.getNeuron(c.endID()), this.layer)); // saving the connections is not necessary, because they are referenced in start and end
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

    public enum LayerType {
        INPUT,
        HIDDEN,
        OUTPUT;
    }

    private LayerType type;

    private int layerID;

    private DoubleFunction<Double> activationFunction;

    private RealMatrix weights;
    private RealVector bias;
    private RealVector activation = null;

    private final List<Neuron> neuronsOfLayer = new ArrayList<>();
    private final List<Neuron> inputNeurons = new ArrayList<>();

    private NeuralNetwork neuralNetwork;

    private NeuralNetworkLayer() {}

    public static NeuralNetworkLayerBuilder build() {
        return new NeuralNetworkLayerBuilder();
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
                .mapToDouble(Neuron::getLastActivation)
                .toArray());
    }

    public void remove(Connection connection) {
        if (!this.neuronsOfLayer.contains(connection.end())) {
            throw new IllegalArgumentException(String.format("Can't remove connection to layer %d from layer %d", connection.end().getLayerID(), getLayerID()));
        }

        setWeightOf(connection, 0);
        // check if neuron is still necessary in the weight matrix
        if (!connection.start().hasConnectionTo(this)) {
            removeInputNeuron(connection.start());
        }
    }

    public void remove(Neuron neuron) {
        // neurons of input and output layer are fixed
        if (this.type != LayerType.HIDDEN) {
            throw new UnsupportedOperationException("Removing neurons from the input or output layer is not supported");
        } else if (!this.neuronsOfLayer.contains(neuron)) {
            throw new IllegalArgumentException(String.format("Can't remove neuron (contained in layer %d) from layer %d", neuron.getLayerID(), getLayerID()));
        }

        // shrink matrices and vectors
        this.weights = MathUtil.removeRowFromMatrix(this.weights, neuron.getInLayerID());
        this.bias = MathUtil.removeElementFromVector(this.bias, neuron.getInLayerID());
        this.activation = MathUtil.removeElementFromVector(this.activation, neuron.getInLayerID());

        // remove neuron and refresh indices of neurons
        this.neuronsOfLayer.remove(neuron);
        IntStream.range(0, this.neuronsOfLayer.size())
                .forEach(i -> this.neuronsOfLayer.get(i).setInLayerID(i));

        // check all input neurons are still necessary
        this.inputNeurons.stream()
                .filter(n -> !n.hasConnectionTo(this))
                .forEach(this::removeInputNeuron);
    }

    private void removeInputNeuron(Neuron start) {
        int neuronIndex = this.inputNeurons.indexOf(start);
        if (neuronIndex != -1) {
            neuronIndex = isInputLayer()
                    ? getNumberOfNeurons() + neuronIndex
                    : neuronIndex;

            this.inputNeurons.remove(neuronIndex);
            this.weights = MathUtil.removeColumnFromMatrix(this.weights, neuronIndex);
        }
    }

    public Neuron addNewNeuron(double bias) {
        // neurons of input and output layer are fixed
        if (this.type != LayerType.HIDDEN) {
            throw new UnsupportedOperationException("Adding neurons to the input or output layer is not supported");
        }

        // Initialize new Neuron
        Neuron newNeuron = new Neuron(this.neuronsOfLayer.size(), this);
        this.neuronsOfLayer.add(newNeuron);

        // growing matrices and vectors
        this.weights = MathUtil.addRowToMatrix(this.weights);
        this.bias = MathUtil.addElementToVector(this.bias);
        this.activation = MathUtil.addElementToVector(this.activation);

        // set bias of new neuron
        this.bias.setEntry(newNeuron.getInLayerID(), bias);

        return newNeuron;
    }

    public void addNewConnection(Neuron start, Neuron end, double weight) {
        if (!this.inputNeurons.contains(start)) {
            // this layer doesn't already have connections to the start neuron --> add it to input neurons and new column to matrix
            this.inputNeurons.add(start);
            this.weights = MathUtil.addColumnToMatrix(this.weights);
        }
        this.weights.setEntry(end.getInLayerID(), this.inputNeurons.indexOf(start), weight);
    }

    public void splitConnection(Connection connection) {
        this.neuralNetwork.splitConnection(connection);
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

    public Stream<Neuron> stream() {
        return this.neuronsOfLayer.stream();
    }

    public int getLayerID() {
        return this.layerID;
    }

    public void setLayerID(int id) {
        this.layerID = id;
    }

    public int getNumberOfNeurons() {
        // use dimension of activation because it always equal to the number of neurons and is initialized before list of neurons
        return this.activation.getDimension();
    }

    public Neuron getNeuron(int neuronIndex) {
        if (neuronIndex > getNumberOfNeurons()) {
            throw new IllegalArgumentException(String.format("Can't find neuron with neuronIndex = %d because the neural network layer only contains %d neurons", neuronIndex, getNumberOfNeurons()));
        }
        return this.neuronsOfLayer.get(neuronIndex);
    }

    public boolean isInputLayer() {
        return this.type == LayerType.INPUT;
    }

    public boolean isOutputLayer() {
        return this.type == LayerType.OUTPUT;
    }

    double getLastActivationOf(int inLayerID) {
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

    public double getWeightOf(Connection connection) {
        if (isInputLayer()) {
            throw new IllegalStateException("Connections to the input layer have no weight");
        }
        return this.weights.getEntry(connection.end().getInLayerID(), connection.start().getInLayerID());
    }

    public void setWeightOf(Connection connection, double weight) {
        if (isInputLayer()) {
            throw new IllegalStateException("Can't change weight of connection to the input layer");
        }
        this.weights.setEntry(connection.end().getInLayerID(), connection.start().getInLayerID(), weight);
    }
}
