package de.emaeuer.ann.impl.neuron.based;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.NeuralNetworkModifier;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;

import java.util.ArrayList;
import java.util.List;

public class NeuronBasedNeuralNetworkModifier implements NeuralNetworkModifier {

    private final NeuronBasedNeuralNetwork nn;

    private final double minWeight;
    private final double maxWeight;

    private Neuron lastModifiedNeuron;

    public NeuronBasedNeuralNetworkModifier(NeuronBasedNeuralNetwork nn, double minWeight, double maxWeight) {
        this.nn = nn;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }
    /**
     * Splits the neuron by adding a new one with a connection to the old one with a weight of 1. All outgoing connections
     * are moved to the new neuron and incoming connections remain unchanged.
     *
     * @param neuronID The neuron to split
     * @return This modifier
     */
    public NeuralNetworkModifier splitNeuron(NeuronID neuronID) {
        Neuron start = this.nn.getNeuron(neuronID);

        addNeuron(1, 0);
        Neuron end = this.lastModifiedNeuron;

        List<Neuron> outputs = new ArrayList<>(start.getOutgoingConnections());

        for (Neuron output : outputs) {
            double weight = output.getWeightOfInput(start);
            output.modify().addInput(end, weight);
            output.modify().removeInput(start);
        }

        end.modify().addInput(start, 1);

        return this;
    }

    @Override
    public NeuralNetworkModifier splitConnection(NeuronID startID, NeuronID endID) {
        Neuron start = nn.getNeuron(startID);
        Neuron end = nn.getNeuron(endID);

        if (!start.hasConnectionTo(end)) {
            throw new IllegalArgumentException(String.format("Can't split non existent connection from %s to %s", startID, endID));
        }

        double weight = end.getWeightOfInput(start);

        addNeuron(1, 0);
        Neuron intermediate = this.lastModifiedNeuron;

        refreshRecurrentIDsIfNecessary(end, intermediate);

        end.modify().addInput(intermediate, 1);
        intermediate.modify().addInput(start, weight);
        end.modify().removeInput(start);

        return this;
    }

    private void refreshRecurrentIDsIfNecessary(Neuron end, Neuron intermediate) {
        boolean recurrentDisabled = this.nn.getConfiguration().getValue(NeuralNetworkConfiguration.DISABLE_RECURRENT_CONNECTIONS, Boolean.class);

        if (!recurrentDisabled || intermediate.getID().getLayerIndex() < end.getID().getLayerIndex()) {
            return;
        }

        intermediate.setRecurrentID(end.getRecurrentID());
        end.modify().increaseRecurrentID();
    }

    @Override
    public NeuralNetworkModifier addConnection(NeuronID startID, NeuronID endID, double weight) {
        Neuron start = nn.getNeuron(startID);
        Neuron end = nn.getNeuron(endID);

        validateRecurrent(startID.getLayerIndex(), endID.getLayerIndex(), start, end);

        end.modify().addInput(start, weight);
        this.lastModifiedNeuron = null;
        return this;
    }

    private void validateRecurrent(int startLayer, int endLayer, Neuron start, Neuron end) {
        boolean recurrentDisabled = this.nn.getConfiguration().getValue(NeuralNetworkConfiguration.DISABLE_RECURRENT_CONNECTIONS, Boolean.class);

        if (recurrentDisabled && startLayer >= endLayer && start.getRecurrentID() >= end.getRecurrentID()) {
            throw new IllegalStateException("Tried to create recurrent connection, but is disabled");
        }
    }

    @Override
    public NeuralNetworkModifier removeConnection(NeuronID startID, NeuronID endID) {
        Neuron start = nn.getNeuron(startID);
        Neuron end = nn.getNeuron(endID);

        end.modify().removeInput(start);
        this.lastModifiedNeuron = null;
        return this;
    }

    @Override
    public NeuralNetworkModifier addNeuron(int layerID, double bias) {
        if (layerID != 1) {
            throw new IllegalArgumentException("Can't add neuron to layer other than hidden layer (index = 1)");
        }

        String activationFunction = this.nn.getConfiguration().getValue(NeuralNetworkConfiguration.HIDDEN_ACTIVATION_FUNCTION, String.class);
        NeuronID id = new NeuronID(1, this.nn.getHiddenNeurons().size());
        Neuron neuron = Neuron.build()
                .type(NeuronType.HIDDEN)
                .activationFunction(ActivationFunction.valueOf(activationFunction))
                .id(id)
                .bias(bias)
                .finish();

        this.nn.getHiddenNeurons().add(neuron);
        this.lastModifiedNeuron = neuron;

        return this;
    }

    @Override
    public NeuralNetworkModifier removeNeuron(NeuronID neuronID) {
        Neuron neuron = this.nn.getNeuron(neuronID);

        if (NeuronType.HIDDEN != neuron.getType()) {
            throw new IllegalArgumentException("Only hidden neurons can be removed");
        }

        int oldNeuronIndex = neuron.getID().getNeuronIndex();
        neuron.modify().disconnectAll();
        this.nn.getHiddenNeurons().remove(neuron);

        // decrease neuron index for all which came later
        this.nn.getHiddenNeurons()
                .stream()
                .skip(oldNeuronIndex)
                .forEach(n -> n.getID().setNeuronIndex(n.getID().getNeuronIndex() - 1));

        this.lastModifiedNeuron = null;

        return this;
    }

    @Override
    public NeuralNetworkModifier setWeightOfConnection(NeuronID startID, NeuronID endID, double weight) {
        if (weight > this.maxWeight || weight < this.minWeight) {
            throw new IllegalArgumentException(String.format("Weight %f is not within bounds [%s:%s]", weight, this.minWeight, this.maxWeight));
        }

        Neuron start = nn.getNeuron(startID);
        Neuron end = nn.getNeuron(endID);

        end.modify().changeWeightOfConnection(start, weight);

        this.lastModifiedNeuron = null;
        return this;
    }

    @Override
    public NeuralNetworkModifier setBiasOfNeuron(NeuronID neuronID, double bias) {
        Neuron neuron = this.nn.getNeuron(neuronID);
        neuron.modify().bias(bias);
        this.lastModifiedNeuron = neuron;
        return this;
    }

    @Override
    public NeuronID getLastModifiedNeuron() {
        return this.lastModifiedNeuron.getID();
    }
}
