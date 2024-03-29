package de.emaeuer.ann.impl.layer.based;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.function.Predicate;

public class NeuralNetworkImpl implements NeuralNetwork {

    private final List<NeuralNetworkLayerImpl> layers = new ArrayList<>();

    private final NeuralNetworkModifierImpl modifier = new NeuralNetworkModifierImpl(this);

    private final ConfigurationHandler<NeuralNetworkConfiguration> configuration;

    private boolean usesExplicitBias = true;

    public NeuralNetworkImpl() {
        this(new ConfigurationHandler<>(NeuralNetworkConfiguration.class));
    }
    public NeuralNetworkImpl(ConfigurationHandler<NeuralNetworkConfiguration> configuration) {
        this.configuration = configuration;
    }

    @Override
    public RealVector process(RealVector input) {
        if (!usesExplicitBias()) {
            // append 1 as activation of on neuron which represents the bias neuron
            input = input.append(1);
        }

        RealVector output = this.layers.get(0).process(input);
        for (int i = 1; i < this.layers.size(); i++) {
            // input vector doesn't have to be passed to the other layers because they retrieve the data from the input layer
            output = this.layers.get(i).process();
        }
        return output;
    }

    @Override
    public NeuralNetworkModifierImpl modify() {
        return this.modifier;
    }

    @Override
    public int getDepth() {
        return this.layers.size();
    }
    @Override
    public List<NeuronID> getOutgoingConnectionsOfNeuron(NeuronID neuron) {
        return this.layers.get(neuron.getLayerIndex())
                .getOutgoingConnectionsOfNeuron(neuron);
    }

    @Override
    public List<NeuronID> getIncomingConnectionsOfNeuron(NeuronID neuron) {
        return this.layers.get(neuron.getLayerIndex())
                .getIncomingConnectionsOfNeuron(neuron);
    }

    @Override
    public boolean neuronHasConnectionTo(NeuronID start, NeuronID end) {
        return getOutgoingConnectionsOfNeuron(start).contains(end);
    }

    @Override
    public boolean neuronHasConnectionToLayer(NeuronID start, int layerIndex) {
        return this.layers.get(start.getLayerIndex())
                .getOutgoingConnectionsOfNeuron(start)
                .stream()
                .anyMatch(n -> n.getLayerIndex() == layerIndex);
    }

    @Override
    public double getWeightOfConnection(NeuronID start, NeuronID end) {
        return this.layers.get(end.getLayerIndex())
                .getWeightOf(start, end);
    }

    @Override
    public void setWeightOfConnection(NeuronID start, NeuronID end, double weight) {
        this.layers.get(end.getLayerIndex())
                .setWeightOf(start, end, weight);
    }

    public double getLastActivationOf(NeuronID neuronID) {
        return this.layers.get(neuronID.getLayerIndex()).getActivationOf(neuronID.getNeuronIndex());
    }

    public NeuralNetworkLayerImpl getLayer(int layerIndex) {
        return this.layers.get(layerIndex);
    }

    public List<NeuralNetworkLayerImpl> getLayers() {
        return this.layers;
    }

    @Override
    public double getBiasOfNeuron(NeuronID neuron) {
        return this.layers.get(neuron.getLayerIndex())
                .getBiasOf(neuron.getNeuronIndex());
    }

    @Override
    public void setBiasOfNeuron(NeuronID neuron, double bias) {
        this.layers.get(neuron.getLayerIndex())
                .setBiasOf(neuron.getNeuronIndex(), bias);
    }

    @Override
    public List<NeuronID> getNeuronsOfLayer(int layerIndex) {
        return this.layers.get(layerIndex).getNeurons();
    }

    @Override
    public NeuralNetwork copy() {
        NeuralNetworkImpl copy = new NeuralNetworkImpl(this.configuration);
        copy.usesExplicitBias = this.usesExplicitBias;

        // saves already created/ copied neurons to prevent duplication of neurons
        Map<NeuronID, NeuronID> existingNeurons = new HashMap<>();

        for (NeuralNetworkLayerImpl layer : this.layers) {
            copy.layers.add(layer.copy(copy, existingNeurons));
        }

        return copy;
    }

    /**
     * Checks if this neuron is contained in the output layer. Doesn't check for valid indices
     * @param neuron to check
     * @return true only if the output layer contains this neuron
     */
    @Override
    public boolean isOutputNeuron(NeuronID neuron) {
        NeuralNetworkLayerImpl outputLayer = this.layers.get(this.layers.size() - 1);
        return outputLayer.getLayerIndex() == neuron.getLayerIndex() && outputLayer.getNumberOfNeurons() > neuron.getNeuronIndex();
    }

    /**
     * Checks if this neuron is contained in the input layer. Doesn't check for valid indices
     * @param neuron to check
     * @return true only if the output layer contains this neuron
     */
    @Override
    public boolean isInputNeuron(NeuronID neuron) {
        NeuralNetworkLayerImpl inputLayer = this.layers.get(0);
        return inputLayer.getLayerIndex() == neuron.getLayerIndex() && inputLayer.getNumberOfNeurons() > neuron.getNeuronIndex();
    }

    @Override
    public boolean usesExplicitBias() {
        return this.usesExplicitBias;
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
        return getLayer(getDepth() - 1).getActivationFunction().getMaxActivation();
    }

    @Override
    public double getMinActivation() {
        return getLayer(getDepth() - 1).getActivationFunction().getMinActivation();
    }

    @Override
    public boolean recurrentIsDisabled() {
        return this.configuration.getValue(NeuralNetworkConfiguration.DISABLE_RECURRENT_CONNECTIONS, Boolean.class);
    }

    @Override
    public int getNumberOfHiddenNeurons() {
        return this.layers.stream()
                .filter(Predicate.not(NeuralNetworkLayerImpl::isInputLayer))
                .filter(Predicate.not(NeuralNetworkLayerImpl::isOutputLayer))
                .mapToInt(NeuralNetworkLayerImpl::getNumberOfNeurons)
                .sum();
    }

    public void setUsesExplicitBias(boolean value) {
        this.usesExplicitBias = value;
    }

    public ConfigurationHandler<NeuralNetworkConfiguration> getConfiguration() {
        return configuration;
    }
}
