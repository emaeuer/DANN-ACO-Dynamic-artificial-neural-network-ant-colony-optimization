package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.Connection;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;
import de.emaeuer.ann.Neuron;
import de.emaeuer.ann.Neuron.NeuronID;

public class NeuralNetworkModifier {

    private final NeuralNetwork nn;

    public NeuralNetworkModifier(NeuralNetwork nn) {
        this.nn = nn;
    }

    public NeuralNetworkModifier splitConnection(Connection connection) {
        Neuron start = connection.start();
        Neuron end = connection.end();

        // initialize intermediateNeuron depending on the positions of start and end
        int layerDistance = end.getLayerID() - start.getLayerID();
        NeuronID intermediateNeuron = Math.abs(layerDistance) == 1
            ? insertNewLayerWithIntermediateNeuron(connection, start, layerDistance)
            : insertIntermediateNeuronToExistingLayer(connection, start, layerDistance);

        // replace old connection with two new ones --> configure weights in a way that the new connections are equivalent to the old one
        // connection from start to intermediateNeuron was already inserted with the weight of the old connection
        end.getContainingLayer()
                .modify()
                .addConnection(intermediateNeuron, end.getNeuronID(), 1);
        connection.delete();

         return this;
    }

    private NeuronID insertIntermediateNeuronToExistingLayer(Connection connection, Neuron start, int layerDistance) {
        // Signum ==  0 --> Neurons are in the same layer (lateral connection) --> add new neuron between them
        // Signum ==  1 --> at least one layer between the neurons (forward connection) --> add neuron to first layer after start
        // Signum == -1 --> at least one layer between the neurons (recurrent connection) --> add neuron to first layer before start
        NeuralNetworkLayer layer = this.nn.getLayers()
                .get(start.getLayerID() + Integer.signum(layerDistance));

        NeuronID neuron = new NeuronID(layer.getLayerID(), layer.getNumberOfNeurons());
        layer.modify()
                .addNeuron(0)
                .addConnection(start.getNeuronID(), neuron, connection.getWeight());
        return neuron;
    }

    private NeuronID insertNewLayerWithIntermediateNeuron(Connection connection, Neuron start, int layerDistance) {
        // Signum ==  1 --> Neurons are in consecutive layers (forward connection) --> add new layer between them
        // Signum == -1 --> Neurons are in consecutive layers (recurrent connection) --> add new layer between them
        // use max(0, layerDistance) because for a recurrent connection (-1) the start and not the output layer should be shifted
        NeuralNetworkLayer layer = addNewLayerAtPosition(start.getLayerID() + Math.max(0, layerDistance), start, connection.getWeight());
        return new NeuronID(layer.getLayerID(), 0); // neuron is only neuron of this layer
    }

    /**
     * Creates a new {@link NeuralNetworkLayerImpl} with one neuron and a connection to it
     * @param position id of the new layer
     * @param connectionStart start neuron of the only connection to this layer
     * @param connectionWeight weight of the connection to this layer
     */
    private NeuralNetworkLayer addNewLayerAtPosition(int position, Neuron connectionStart, double connectionWeight) {
        // increase indices of all following layers by one
        this.nn.getLayers().subList(position, this.nn.getLayers().size())
                .forEach(layer -> ((NeuralNetworkLayerImpl) layer).setLayerID(layer.getLayerID() + 1));

        // add placeholder at the position of the new layer so that the indexing after shifting still works
        this.nn.getLayers().add(position, null);

        NeuralNetworkLayer newLayer = NeuralNetworkLayer.build()
                .numberOfNeurons(1)
                .layerType(NeuralNetworkLayerImpl.LayerType.HIDDEN)
                .neuralNetwork(this.nn)
                .layerID(position)
                .addConnection(new Connection.ConnectionPrototype(connectionStart.getNeuronID(), new NeuronID(position, 0), connectionWeight))
                .finish();

        this.nn.getLayers().set(position, newLayer);

        return newLayer;
    }

    public NeuralNetworkModifier addConnection(NeuronID startID, NeuronID endID, double weight) {
        this.nn.getLayer(endID.layerID())
                .modify()
                .addConnection(startID, endID, weight);

        return this;
    }

    public NeuralNetworkModifier removeConnection(NeuronID startID, NeuronID endID) {
        this.nn.getLayer(endID.layerID())
                .modify()
                .removeConnection(startID, endID);

        return this;
    }

    public NeuralNetworkModifier addNeuron(int layerID, double bias) {
        this.nn.getLayer(layerID)
                .modify()
                .addNeuron(bias);

        return this;
    }

    public NeuralNetworkModifier removeNeuron(NeuronID neuron) {
        this.nn.getLayer(neuron.layerID())
                .modify()
                .removeNeuron(neuron.neuronID());

        return this;
    }
}
