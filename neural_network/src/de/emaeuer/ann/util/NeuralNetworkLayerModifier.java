package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.Connection;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;
import de.emaeuer.ann.Neuron;

import java.util.stream.IntStream;

public class NeuralNetworkLayerModifier {

    private final NeuralNetworkLayerImpl layer;

    public NeuralNetworkLayerModifier(NeuralNetworkLayerImpl layer) {
        this.layer = layer;
    }

    public NeuralNetworkLayerModifier removeConnection(Neuron.NeuronID startID, Neuron.NeuronID endID) {
        Neuron start = this.layer.getNeuralNetwork().getNeuron(startID);
        Neuron end = this.layer.getNeuralNetwork().getNeuron(endID);

        Connection connection = start.getConnectionTo(end);

        if (connection == null) {
            throw new IllegalArgumentException(String.format("Can't remove not existing connection from %s to %s", start.getNeuronID(), end.getNeuronID()));
        }

        removeConnection(connection);

        return this;
    }

    public NeuralNetworkLayerModifier removeConnection(Connection connection) {
        if (!this.layer.getNeurons().contains(connection.end())) {
            throw new IllegalArgumentException(String.format("Can't remove connection to layer %d from layer %d", connection.end().getLayerID(), this.layer.getLayerID()));
        }

        connection.start().getOutgoingConnections().remove(connection);
        connection.end().getIncomingConnections().remove(connection);

        // update the weight matrix
        if (!connection.start().hasConnectionTo(this.layer)) {
            // check if the input neuron is still necessary
            removeInputNeuron(connection.start());
        } else {
            this.layer.setWeightOf(connection, 0);
        }

        return this;
    }

    public NeuralNetworkLayerModifier removeNeuron(int inLayerID) {
        removeNeuron(this.layer.getNeuron(inLayerID));
        return this;
    }

    private NeuralNetworkLayerModifier removeNeuron(Neuron neuron) {
        // neurons of input and output layer are fixed
        if (this.layer.getType() != NeuralNetworkLayer.LayerType.HIDDEN) {
            throw new UnsupportedOperationException("Removing neurons from the input or output layer is not supported");
        } else if (this.layer.getNumberOfNeurons() == 1) {
            // TODO A automatically delete the layer and throw exception if layers are not connected anymore
            // TODO B throw custom exception, catch and remove afterwards (kind of confirmation)
            throw new IllegalStateException("Removing the neuron isn't possible because it is the last one in this layer. Delete the layer instead");
        }

        deleteAllIncomingAndOutgoingConnectionsOfNeuron(neuron);
        shrinkWeightsBiasAndActivation(neuron);
        removeNeuronAndShiftRemainingNeuronsAccordingly(neuron);
        checkAllInputNeuronsAreNecessary();

        return this;
    }

    public NeuralNetworkLayerModifier addNeuron(double bias) {
        // neurons of input and output layer are fixed
        if (this.layer.getType() != NeuralNetworkLayer.LayerType.HIDDEN) {
            throw new UnsupportedOperationException("Adding neurons to the input or output layer is not supported");
        }

        growWeightsBiasAndActivation();
        createNewNeuron(bias);

        return this;
    }

    public NeuralNetworkLayerModifier addConnection(Neuron.NeuronID startID, Neuron.NeuronID endID, double weight) {
        Neuron start = this.layer.getNeuralNetwork().getNeuron(startID);
        Neuron end = this.layer.getNeuralNetwork().getNeuron(endID);

        addConnection(start, end, weight);

        return this;
    }

    public NeuralNetworkLayerModifier addConnection(Neuron start, Neuron end, double weight) {
        if (!this.layer.getNeurons().contains(end)) {
            throw new IllegalArgumentException(String.format("Can't add a connection to neuron %s in layer %d", end.getNeuronID(), this.layer.getLayerID()));
        } else if (start.hasConnectionTo(end)) {
            throw new IllegalStateException(String.format("The connection from neuron %s to %s already exists", start.getNeuronID(), end.getNeuronID()));
        }

        if (!this.layer.getInputNeurons().contains(start)) {
            // this layer doesn't already have connections to the start neuron --> add it to input neurons and new column to matrix
            this.layer.getInputNeurons().add(start);
            this.layer.setWeights(MathUtil.addColumnToMatrix(this.layer.getWeights()));
        }
        this.layer.getWeights().setEntry(end.getNeuronInLayerID(), this.layer.getInputNeurons().indexOf(start), weight);

        Connection connection = new Connection(start, end);
        start.getOutgoingConnections().add(connection);
        end.getIncomingConnections().add(connection);

        return this;
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    private void shrinkWeightsBiasAndActivation(Neuron neuron) {
        this.layer.setWeights(MathUtil.removeRowFromMatrix(this.layer.getWeights(), neuron.getNeuronInLayerID()));
        this.layer.setBias(MathUtil.removeElementFromVector(this.layer.getBias(), neuron.getNeuronInLayerID()));
        this.layer.setActivation(MathUtil.removeElementFromVector(this.layer.getActivation(), neuron.getNeuronInLayerID()));
    }

    private void growWeightsBiasAndActivation() {
        this.layer.setWeights(MathUtil.addRowToMatrix(this.layer.getWeights()));
        this.layer.setBias(MathUtil.addElementToVector(this.layer.getBias()));
        this.layer.setActivation(MathUtil.addElementToVector(this.layer.getActivation()));
    }

    private void createNewNeuron(double bias) {
        // Initialize new Neuron
        // inLayerID -1 because the size of the layer was already increased
        Neuron newNeuron = new Neuron(this.layer.getNumberOfNeurons() - 1, this.layer);
        this.layer.getNeurons().add(newNeuron);

        // set bias of new neuron
        this.layer.getBias().setEntry(newNeuron.getNeuronInLayerID(), bias);
    }

    private void deleteAllIncomingAndOutgoingConnectionsOfNeuron(Neuron neuron) {
        while (!neuron.getIncomingConnections().isEmpty()) {
            neuron.getIncomingConnections().get(0).delete();
        }
        while (!neuron.getOutgoingConnections().isEmpty()) {
            neuron.getOutgoingConnections().get(0).delete();
        }
    }

    private void removeNeuronAndShiftRemainingNeuronsAccordingly(Neuron neuron) {
        // remove neuron and refresh indices of neurons
        this.layer.getNeurons().remove(neuron);
        IntStream.range(neuron.getNeuronInLayerID(), this.layer.getNumberOfNeurons())
                .forEach(i -> this.layer.getNeurons().get(i).setInLayerID(i));
    }

    private void checkAllInputNeuronsAreNecessary() {
        this.layer.getNeurons().stream()
                .filter(n -> !n.hasConnectionTo(this.layer))
                .forEach(this::removeInputNeuron);
    }

    private void removeInputNeuron(Neuron start) {
        int neuronIndex = this.layer.getInputNeurons().indexOf(start);
        if (neuronIndex != -1) {
            neuronIndex = this.layer.isInputLayer()
                    ? this.layer.getNumberOfNeurons() + neuronIndex
                    : neuronIndex;

            this.layer.getInputNeurons().remove(neuronIndex);
            this.layer.setWeights(MathUtil.removeColumnFromMatrix(this.layer.getWeights(), neuronIndex));
        }
    }

}
