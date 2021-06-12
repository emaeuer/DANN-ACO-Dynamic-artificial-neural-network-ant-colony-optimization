package de.emaeuer.ann.impl.layer.based;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.LayerType;
import de.emaeuer.ann.NeuralNetworkModifier;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;

public class NeuralNetworkModifierImpl implements NeuralNetworkModifier {

    private final NeuralNetworkImpl nn;

    public NeuralNetworkModifierImpl(NeuralNetworkImpl nn) {
        this.nn = nn;
    }

    private NeuronID lastModifiedNeuron;

    @Override
    public NeuralNetworkModifierImpl splitConnection(NeuronID startID, NeuronID endID) {
        NeuronID start = getReferenceToCorrespondingNeuronID(startID);
        NeuronID end = getReferenceToCorrespondingNeuronID(endID);

        // initialize intermediateNeuron depending on the positions of start and end
        int layerDistance = end.getLayerIndex() - start.getLayerIndex();
        NeuronID intermediateNeuron = Math.abs(layerDistance) == 1
                ? insertNewLayerWithIntermediateNeuron(start, end, layerDistance)
                : insertIntermediateNeuronToExistingLayer(start, end, layerDistance);

        // replace old connection with two new ones --> configure weights in a way that the new connections are equivalent to the old one
        // connection from start to intermediateNeuron was already inserted with the weight of the old connection
        addConnection(intermediateNeuron, end, 1);
        removeConnection(start, end);

        // refresh passed start and end
        startID.setLayerIndex(start.getLayerIndex());
        startID.setNeuronIndex(start.getNeuronIndex());
        endID.setLayerIndex(end.getLayerIndex());
        endID.setNeuronIndex(end.getNeuronIndex());

        this.lastModifiedNeuron = intermediateNeuron;

        return this;
    }

    private NeuronID insertIntermediateNeuronToExistingLayer(NeuronID start, NeuronID end, int layerDistance) {
        // Signum ==  0 --> Neurons are in the same layer (lateral connection) --> add new neuron between them
        // Signum ==  1 --> at least one layer between the neurons (forward connection) --> add neuron to first layer after start
        // Signum == -1 --> at least one layer between the neurons (recurrent connection) --> add neuron to first layer before start
        NeuralNetworkLayerImpl layer = this.nn.getLayers()
                .get(start.getLayerIndex() + Integer.signum(layerDistance));

        layer.modify().addNeuron(0);

        // new neuron is last in the respective layer
        NeuronID newEnd = layer.getNeurons().get(layer.getNumberOfNeurons() - 1);

        layer.modify().addConnection(start, newEnd, this.nn.getWeightOfConnection(start, end));

        return newEnd;
    }

    private NeuronID insertNewLayerWithIntermediateNeuron(NeuronID start, NeuronID end, int layerDistance) {
        // Signum ==  1 --> Neurons are in consecutive layers (forward connection) --> add new layer between them
        // Signum == -1 --> Neurons are in consecutive layers (recurrent connection) --> add new layer between them
        // use max(0, layerDistance) because for a recurrent connection (-1) the start and not the output layer should be shifted
        double connectionWeight = this.nn.getWeightOfConnection(start, end);
        NeuralNetworkLayerImpl layer = addNewLayerAtPosition(start.getLayerIndex() + Math.max(0, layerDistance), start, connectionWeight);
        return layer.getNeurons().get(0); // neuron is only neuron of this layer
    }

    /**
     * Creates a new {@link NeuralNetworkLayerImpl} with one neuron and a connection to it
     * @param position id of the new layer
     * @param start start neuron of the only connection to this layer
     * @param connectionWeight weight of the connection to this layer
     */
    private NeuralNetworkLayerImpl addNewLayerAtPosition(int position, NeuronID start, double connectionWeight) {
        // increase indices of all following layers by one
        this.nn.getLayers().subList(position, this.nn.getLayers().size())
                .forEach(layer -> layer.setLayerIndex(layer.getLayerIndex() + 1));

        // add placeholder at the position of the new layer so that the indexing after shifting still works
        this.nn.getLayers().add(position, null);

        NeuralNetworkLayerBuilderImpl builder = NeuralNetworkLayerImpl.build();

        String activationFunction = this.nn.getConfiguration().getValue(NeuralNetworkConfiguration.HIDDEN_ACTIVATION_FUNCTION, String.class);
        double maxWeight = this.nn.getConfiguration().getValue(NeuralNetworkConfiguration.WEIGHT_MAX, Double.class);
        double minWeight = this.nn.getConfiguration().getValue(NeuralNetworkConfiguration.WEIGHT_MIN, Double.class);

        NeuralNetworkLayerImpl newLayer = builder
                .neuralNetwork(this.nn)
                .activationFunction(ActivationFunction.valueOf(activationFunction))
                .layerType(LayerType.HIDDEN)
                .maxWeight(maxWeight)
                .minWeight(minWeight)
                .layerID(position)
                .numberOfNeurons(1)
                .addConnection(start, new NeuronID(position, 0), connectionWeight)
                .finish();

        this.nn.getLayers().set(position, newLayer);

        return newLayer;
    }

    @Override
    public NeuralNetworkModifierImpl addConnection(NeuronID startID, NeuronID endID, double weight) {
        NeuronID start = getReferenceToCorrespondingNeuronID(startID);
        NeuronID end = getReferenceToCorrespondingNeuronID(endID);

        this.nn.getLayer(endID.getLayerIndex())
                .modify()
                .addConnection(start, end, weight);

        return this;
    }

    @Override
    public NeuralNetworkModifierImpl removeConnection(NeuronID startID, NeuronID endID) {
        startID = getReferenceToCorrespondingNeuronID(startID);
        endID = getReferenceToCorrespondingNeuronID(endID);

        this.nn.getLayer(endID.getLayerIndex())
                .modify()
                .removeConnection(startID, endID);

        return this;
    }

    @Override
    public NeuralNetworkModifierImpl addNeuron(int layerID, double bias) {
        NeuralNetworkLayerImpl layer = this.nn.getLayer(layerID);

        layer.modify()
                .addNeuron(bias);


        this.lastModifiedNeuron = layer.getNeurons().get(layer.getNumberOfNeurons() - 1);

        return this;
    }

    @Override
    public NeuralNetworkModifierImpl removeNeuron(NeuronID neuron) {
        neuron = getReferenceToCorrespondingNeuronID(neuron);
        this.nn.getLayer(neuron.getLayerIndex())
                .modify()
                .removeNeuron(neuron);

        this.lastModifiedNeuron = neuron;

        return this;
    }

    @Override
    public NeuralNetworkModifier setWeightOfConnection(NeuronID startID, NeuronID endID, double weight) {
        NeuronID start = getReferenceToCorrespondingNeuronID(startID);
        NeuronID end = getReferenceToCorrespondingNeuronID(endID);

        this.nn.setWeightOfConnection(start, end, weight);

        return this;
    }

    @Override
    public NeuralNetworkModifier setBiasOfNeuron(NeuronID neuronID, double bias) {
        NeuronID neuron = getReferenceToCorrespondingNeuronID(neuronID);

        this.nn.setBiasOfNeuron(neuron, bias);

        return this;
    }

    /**
     * Finds the corresponding {@link NeuronID} object int the neural network and returns the reference to this one
     * @param other NeuronID
     * @return reference to the corresponding {@link NeuronID} in this network
     */
    private NeuronID getReferenceToCorrespondingNeuronID(NeuronID other) {
        return this.nn.getLayer(other.getLayerIndex()).getNeurons()
                .get(other.getNeuronIndex());
    }

    @Override
    public NeuronID getLastModifiedNeuron() {
        return this.lastModifiedNeuron;
    }
}
