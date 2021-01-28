package de.emaeuer.ann.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

public class NeuralNetworkImpl implements NeuralNetwork {

    private final List<NeuralNetworkLayerImpl> layers = new ArrayList<>();

    private final NeuralNetworkModifierImpl modifier = new NeuralNetworkModifierImpl(this);

    @Override
    public RealVector process(RealVector input) {
        RealVector output = this.layers.get(0).process(input);
        for (int i = 1; i < this.layers.size(); i++) {
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
}
