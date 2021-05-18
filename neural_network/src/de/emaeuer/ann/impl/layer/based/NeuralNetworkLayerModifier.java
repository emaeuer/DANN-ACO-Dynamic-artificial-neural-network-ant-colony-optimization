package de.emaeuer.ann.impl.layer.based;

import de.emaeuer.ann.LayerType;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.util.MathUtil;

import java.util.List;
import java.util.stream.IntStream;

public class NeuralNetworkLayerModifier {

    private final NeuralNetworkLayerImpl layer;

    public NeuralNetworkLayerModifier(NeuralNetworkLayerImpl layer) {
        this.layer = layer;
    }

    public NeuralNetworkLayerModifier removeConnection(NeuronID start, NeuronID end) {
        if (!this.layer.getNeurons().contains(end)) {
            throw new IllegalArgumentException(String.format("Can't remove connection to layer %d from layer %d", end.getLayerIndex(), this.layer.getLayerIndex()));
        }

        // unregister the connection
        this.layer.getIncomingConnectionsOfNeuron(end).remove(start);
        this.layer.getNeuralNetwork().getLayer(start.getLayerIndex()).getOutgoingConnectionsOfNeuron(start).remove(end);

        // update the weight matrix
        if (!this.layer.getNeuralNetwork().neuronHasConnectionToLayer(start, this.layer.getLayerIndex())) {
            // check if the input neuron is still necessary
            removeInputNeuron(start);
        } else {
            this.layer.setWeightOf(start, end, 0);
        }

        return this;
    }

    public NeuralNetworkLayerModifier removeNeuron(NeuronID neuron) {
        // neurons of input and output layer are fixed
        if (this.layer.getType() != LayerType.HIDDEN) {
            throw new UnsupportedOperationException("Removing neurons from the input or output layer is not supported");
        } else if (this.layer.getNumberOfNeurons() == 1) {
            // TODO A automatically delete the layer and throw exception if layers are not connected anymore
            // TODO B throw custom exception, catch and remove afterwards (kind of confirmation)
            throw new IllegalStateException("Removing the neuron isn't possible because it is the last one in this layer. Delete the layer instead");
        } else if (neuron.getLayerIndex() != this.layer.getLayerIndex()) {
            throw new IllegalArgumentException(String.format("Can't remove neuron %s from layer %s", neuron, this.layer.getLayerIndex()));
        }

        deleteAllIncomingAndOutgoingConnectionsOfNeuron(neuron);
        shrinkWeightsBiasAndActivation(neuron);
        removeNeuronAndShiftRemainingNeuronsAccordingly(neuron);
        checkAllInputNeuronsAreNecessary();

        return this;
    }

    public NeuralNetworkLayerModifier addNeuron(double bias) {
        // neurons of input and output layer are fixed
        if (this.layer.getType() != LayerType.HIDDEN) {
            throw new UnsupportedOperationException("Adding neurons to the input or output layer is not supported");
        }

        growWeightsBiasAndActivation();
        createNewNeuron(bias);

        return this;
    }

    public NeuralNetworkLayerModifier addConnection(NeuronID start, NeuronID end, double weight) {
        if (!this.layer.getNeurons().contains(end)) {
            throw new IllegalArgumentException(String.format("Can't add a connection to neuron %s in layer %d", end, this.layer.getLayerIndex()));
        } else if (this.layer.getNeuralNetwork().neuronHasConnectionTo(start, end)) {
            throw new IllegalStateException(String.format("The connection from neuron %s to %s already exists", start, end));
        }

        end = this.layer.getNeurons().get(end.getNeuronIndex());

        if (!this.layer.getInputNeurons().contains(start)) {
            // this layer doesn't already have connections to the start neuron --> add it to input neurons and new column to matrix
            this.layer.getInputNeurons().add(start);
            this.layer.setWeights(MathUtil.addColumnToMatrix(this.layer.getWeights()));
        }
        this.layer.getWeights().setEntry(end.getNeuronIndex(), this.layer.getInputNeurons().indexOf(start), weight);

        this.layer.addIncomingConnection(start, end);
        this.layer.getNeuralNetwork().getLayer(start.getLayerIndex()).addOutgoingConnection(start, end);

        return this;
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    private void shrinkWeightsBiasAndActivation(NeuronID neuron) {
        this.layer.setWeights(MathUtil.removeRowFromMatrix(this.layer.getWeights(), neuron.getNeuronIndex()));
        this.layer.setBias(MathUtil.removeElementFromVector(this.layer.getBias(), neuron.getNeuronIndex()));
        this.layer.setActivation(MathUtil.removeElementFromVector(this.layer.getActivation(), neuron.getNeuronIndex()));
    }

    private void growWeightsBiasAndActivation() {
        this.layer.setWeights(MathUtil.addRowToMatrix(this.layer.getWeights()));
        if (this.layer.getNeuralNetwork().usesExplicitBias()) {
            this.layer.setBias(MathUtil.addElementToVector(this.layer.getBias()));
        }
        this.layer.setActivation(MathUtil.addElementToVector(this.layer.getActivation()));
    }

    private void createNewNeuron(double bias) {
        // Initialize new Neuron
        // inLayerID -1 because the size of the layer was already increased
        NeuronID newNeuron = new NeuronID(this.layer.getLayerIndex(), this.layer.getNumberOfNeurons() - 1);
        this.layer.getNeurons().add(newNeuron);

        // set bias of new neuron
        if (this.layer.getNeuralNetwork().usesExplicitBias()) {
            this.layer.getBias().setEntry(newNeuron.getNeuronIndex(), bias);
        }
    }

    private void deleteAllIncomingAndOutgoingConnectionsOfNeuron(NeuronID neuron) {
        NeuralNetworkModifierImpl modifier = this.layer.getNeuralNetwork().modify();

        List<NeuronID> incomingConnectionsToRemove = this.layer.getIncomingConnectionsOfNeuron(neuron);
        while (!incomingConnectionsToRemove.isEmpty()) {
            modifier.removeConnection(incomingConnectionsToRemove.get(0), neuron);
        }

        List<NeuronID> outgoingConnectionsToRemove = this.layer.getOutgoingConnectionsOfNeuron(neuron);
        while (!outgoingConnectionsToRemove.isEmpty()) {
            modifier.removeConnection(neuron, outgoingConnectionsToRemove.get(0));
        }
    }

    private void removeNeuronAndShiftRemainingNeuronsAccordingly(NeuronID neuron) {
        // remove neuron and refresh indices of neurons
        this.layer.getNeurons().remove(neuron);
        IntStream.range(neuron.getNeuronIndex(), this.layer.getNumberOfNeurons())
                .mapToObj(i -> this.layer.getNeurons().get(i))
                .forEach(n -> applyNeuronIDChange(n, new NeuronID(n.getLayerIndex(), n.getNeuronIndex() - 1)));
    }

    public void applyNeuronIDChange(NeuronID oldValue, NeuronID newValue) {
        List<NeuronID> outgoingConnections = this.layer.getOutgoingConnectionsOfNeuron(oldValue);
        List<NeuronID> incomingConnections = this.layer.getIncomingConnectionsOfNeuron(oldValue);

        // the keys of the map have to be replaced by the hash value of the shifted neuron
        this.layer.getIncomingConnections().remove(oldValue);
        this.layer.getOutgoingConnections().remove(oldValue);

        // apply changes to the oldValue to keep all references to the old value
        oldValue.setLayerIndex(newValue.getLayerIndex());
        oldValue.setNeuronIndex(newValue.getNeuronIndex());

        // add the connections under the new key (only if the key existed previously)
        if (!outgoingConnections.isEmpty()) {
            this.layer.getOutgoingConnections().put(oldValue, outgoingConnections);
        }
        if (!incomingConnections.isEmpty()) {
            this.layer.getIncomingConnections().put(oldValue, incomingConnections);
        }
    }

    private void checkAllInputNeuronsAreNecessary() {
        this.layer.getInputNeurons().stream()
                .filter(n -> !this.layer.getNeuralNetwork().neuronHasConnectionToLayer(n, this.layer.getLayerIndex()))
                .forEach(this::removeInputNeuron);
    }

    private void removeInputNeuron(NeuronID start) {
        int neuronIndex = this.layer.getInputNeurons().indexOf(start);
        if (neuronIndex != -1) {
            this.layer.getInputNeurons().remove(neuronIndex);
            this.layer.setWeights(MathUtil.removeColumnFromMatrix(this.layer.getWeights(), neuronIndex));
        }
    }



}
