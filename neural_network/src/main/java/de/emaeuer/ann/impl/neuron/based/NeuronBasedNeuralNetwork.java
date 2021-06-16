package de.emaeuer.ann.impl.neuron.based;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkModifier;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NeuronBasedNeuralNetwork implements NeuralNetwork {

    private final NeuronBasedNeuralNetworkModifier modifier;

    private final ConfigurationHandler<NeuralNetworkConfiguration> configuration;

    private final Neuron biasNeuron;
    private final List<Neuron> inputNeurons;
    private final List<Neuron> hiddenNeurons;
    private final List<Neuron> outputNeurons;

    public NeuronBasedNeuralNetwork(ConfigurationHandler<NeuralNetworkConfiguration> configuration, Neuron biasNeuron, List<Neuron> inputLayer, List<Neuron> hiddenLayer, List<Neuron> outputLayer) {
        this.configuration = configuration;

        this.biasNeuron = biasNeuron;
        this.inputNeurons = inputLayer;
        this.hiddenNeurons = hiddenLayer;
        this.outputNeurons = outputLayer;

        double maxWeight = this.configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MAX, Double.class);
        double minWeight = this.configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MIN, Double.class);
        this.modifier = new NeuronBasedNeuralNetworkModifier(this, minWeight, maxWeight);
    }

    /**
     * Constructor for copying only
     */
    private NeuronBasedNeuralNetwork(NeuronBasedNeuralNetwork other) {
        this.configuration = other.configuration;

        double maxWeight = this.configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MAX, Double.class);
        double minWeight = this.configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MIN, Double.class);
        this.modifier = new NeuronBasedNeuralNetworkModifier(this, minWeight, maxWeight);

        this.biasNeuron = other.biasNeuron == null ? null : other.biasNeuron.copyWithoutConnections();

        // copy all neurons without connections
        this.inputNeurons = other.inputNeurons.stream()
                .map(Neuron::copyWithoutConnections)
                .collect(Collectors.toCollection(ArrayList::new));
        this.hiddenNeurons = other.hiddenNeurons.stream()
                .map(Neuron::copyWithoutConnections)
                .collect(Collectors.toCollection(ArrayList::new));
        this.outputNeurons = other.outputNeurons.stream()
                .map(Neuron::copyWithoutConnections)
                .collect(Collectors.toCollection(ArrayList::new));

        // add all connections
        NeuralNetworkUtil.iterateNeuralNetworkConnections(other)
                .forEachRemaining(c -> this.getNeuron(c.end()).modify().addInput(this.getNeuron(c.start()), c.weight()));
    }

    @Override
    public RealVector process(RealVector input) {
        if (input.getDimension() != inputNeurons.size()) {
            throw new IllegalArgumentException(String.format(
                    "The size of the input vector %d doesn't match the number of input neurons %d",
                    input.getDimension(), inputNeurons.size()));
        }

        IntStream.range(0, input.getDimension())
                .forEach(i -> inputNeurons.get(i).activate(input.getEntry(i)));

        this.hiddenNeurons.forEach(Neuron::activate);
        this.outputNeurons.forEach(Neuron::activate);

        this.hiddenNeurons.forEach(Neuron::reactivate);
        this.outputNeurons.forEach(Neuron::reactivate);

        double[] result = outputNeurons.stream()
                .mapToDouble(Neuron::getActivation)
                .toArray();

        return new ArrayRealVector(result);
    }

    @Override
    public NeuralNetworkModifier modify() {
        return this.modifier;
    }

    @Override
    public int getDepth() {
        return this.hiddenNeurons.isEmpty() ? 2 : 3;
    }

    @Override
    public List<NeuronID> getOutgoingConnectionsOfNeuron(NeuronID neuronID) {
        Neuron neuron = getNeuron(neuronID);

        return neuron.getOutgoingConnections()
                .stream()
                .map(Neuron::getID)
                .collect(Collectors.toList());
    }

    @Override
    public List<NeuronID> getIncomingConnectionsOfNeuron(NeuronID neuronID) {
        Neuron neuron = getNeuron(neuronID);

        return neuron.getIncomingConnections()
                .stream()
                .map(Neuron::getID)
                .collect(Collectors.toList());
    }

    @Override
    public boolean neuronHasConnectionTo(NeuronID startID, NeuronID endID) {
        Neuron start = getNeuron(startID);
        Neuron end = getNeuron(endID);

        return start.hasConnectionTo(end);
    }

    @Override
    public boolean neuronHasConnectionToLayer(NeuronID neuronID, int layerIndex) {
        return getOutgoingConnectionsOfNeuron(neuronID)
            .stream()
            .map(NeuronID::getLayerIndex)
            .anyMatch(l -> l == layerIndex);
    }

    @Override
    public double getWeightOfConnection(NeuronID startID, NeuronID endID) {
        Neuron start = getNeuron(startID);
        Neuron end = getNeuron(endID);

        return end.getWeightOfInput(start);
    }

    @Override
    public void setWeightOfConnection(NeuronID startID, NeuronID endID, double weight) {
        Neuron start = getNeuron(startID);
        Neuron end = getNeuron(endID);

        end.modify().changeWeightOfConnection(start, weight);
    }

    @Override
    public double getBiasOfNeuron(NeuronID neuronID) {
        Neuron neuron = getNeuron(neuronID);
        return neuron.getBias();
    }

    @Override
    public void setBiasOfNeuron(NeuronID neuronID, double biasValue) {
        Neuron neuron = getNeuron(neuronID);
        neuron.modify().bias(biasValue);
    }

    @Override
    public List<NeuronID> getNeuronsOfLayer(int layerIndex) {
        if (layerIndex > getDepth() || layerIndex < 0) {
            throw new IndexOutOfBoundsException(String.format("The layer index %d is invalid for a neural network which only consists of 3 layers", layerIndex));
        }

        List<Neuron> neurons = switch (layerIndex) {
            case 0 -> this.inputNeurons;
            case 1 -> this.getDepth() == 3 ? this.hiddenNeurons : this.outputNeurons;
            case 2 -> this.outputNeurons;
            default -> Collections.emptyList();
        };

        // copy list to prevent external modification
        neurons = new ArrayList<>(neurons);

        if (!usesExplicitBias() && layerIndex == 0) {
            neurons.add(0, this.biasNeuron);
        }

        return neurons.stream()
                .map(Neuron::getID)
                .collect(Collectors.toList());
    }

    @Override
    public NeuralNetwork copy() {
        return new NeuronBasedNeuralNetwork(this);
    }

    @Override
    public boolean isOutputNeuron(NeuronID currentNeuron) {
        return currentNeuron.getLayerIndex() == 2;
    }

    @Override
    public boolean isInputNeuron(NeuronID currentNeuron) {
        return currentNeuron.getLayerIndex() == 0;
    }

    public List<Neuron> getHiddenNeurons() {
        return this.hiddenNeurons;
    }

    public ConfigurationHandler<NeuralNetworkConfiguration> getConfiguration() {
        return this.configuration;
    }

    @Override
    public boolean usesExplicitBias() {
        return this.biasNeuron == null;
    }

    @Override
    public double getMaxWeightValue() {
        return this.configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MAX, Double.class);
    }

    @Override
    public double getMinWeightValue() {
        return this.configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MIN, Double.class);
    }

    @Override
    public double getMaxActivation() {
        Neuron output = this.outputNeurons.get(0);
        return output.getActivationFunction().getMaxActivation();
    }

    @Override
    public double getMinActivation() {
        Neuron output = this.outputNeurons.get(0);
        return output.getActivationFunction().getMinActivation();
    }

    @Override
    public boolean recurrentIsDisabled() {
        return this.configuration.getValue(NeuralNetworkConfiguration.DISABLE_RECURRENT_CONNECTIONS, Boolean.class);
    }

    public Neuron getNeuron(NeuronID id) {
        // if the neural network uses an on-neuron as bias the 0-0 neuron is the bias neuron
        if (id.getLayerIndex() == 0 && !usesExplicitBias()) {
            if (id.getNeuronIndex() == 0) {
                return this.biasNeuron;
            } else {
                return this.inputNeurons.get(id.getNeuronIndex() - 1);
            }
        } else if (id.getLayerIndex() == 0) {
            return this.inputNeurons.get(id.getNeuronIndex());
        } else if (id.getLayerIndex() == 1) {
            return this.hiddenNeurons.get(id.getNeuronIndex());
        } else if (id.getLayerIndex() == 2) {
            return this.outputNeurons.get(id.getNeuronIndex());
        }

        throw new IndexOutOfBoundsException(String.format("Can't access neurons of layer %d", id.getLayerIndex()));
    }

}
