package de.emaeuer.ann2;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NeuralNetwork {

    public static class NeuralNetworkBuilder {

        private final NeuralNetwork nn = new NeuralNetwork();

        private boolean nextFullyConnected = false;

        private NeuralNetworkBuilder() {
        };

        public NeuralNetworkBuilder inputLayer(int size) {
            return inputLayer(b -> b.numberOfNeurons(size));
        }

        /**
         * Builds a neural network input layer. Automatically defines and overwrites
         * if present the layer type, the layer id and the neural network of the neural
         * network layer builder.
         *
         * @throws IllegalStateException if this method was called previously
         * @param modifier for a neural network layer builder
         * @return this builder
         */
        public NeuralNetworkBuilder inputLayer(Consumer<NeuralNetworkLayer.NeuralNetworkLayerBuilder> modifier) {
            this.nextFullyConnected = false;

            NeuralNetworkLayer.NeuralNetworkLayerBuilder builder = NeuralNetworkLayer.build();
            modifier.andThen(b -> b.neuralNetwork(this.nn)
                    .layerID(0)
                    .layerType(NeuralNetworkLayer.LayerType.INPUT))
                .accept(builder);

            try {
                this.nn.layers.add(builder.finish());
            } catch (NotStrictlyPositiveException e) {
                throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
            }

            return this;
        }

        public NeuralNetworkBuilder hiddenLayer(int size) {
            return hiddenLayer(b -> b.numberOfNeurons(size));
        }

        /**
         * Builds a neural network hidden layer. Automatically defines and overwrites
         * if present the layer type, the layer id and the neural network of the neural
         * network layer builder.
         *
         * @throws IllegalStateException if this method was called before inputLayer or after outputLayer
         * @param modifier for a neural network layer builder
         * @return this builder
         */
        public NeuralNetworkBuilder hiddenLayer(Consumer<NeuralNetworkLayer.NeuralNetworkLayerBuilder> modifier) {
            modifier = checkAndFullyConnectToPreviousLayer(modifier);

            NeuralNetworkLayer.NeuralNetworkLayerBuilder builder = NeuralNetworkLayer.build();
            modifier.andThen(b -> b.neuralNetwork(this.nn)
                    .layerID(this.nn.layers.size())
                    .layerType(NeuralNetworkLayer.LayerType.HIDDEN))
                    .accept(builder);

            try {
                this.nn.layers.add(builder.finish());
            } catch (NotStrictlyPositiveException e) {
                throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
            }

            return this;
        }

        public NeuralNetworkBuilder outputLayer(int size) {
            return outputLayer(b -> b.numberOfNeurons(size));
        }

        /**
         * Builds a neural network output layer. Automatically defines and overwrites
         * if present the layer type, the layer id and the neural network of the neural
         * network layer builder.
         *
         * @throws IllegalStateException if this method was called previously or before inputLayer
         * @throws  IllegalArgumentException if the modifier doesn't contain connection definitions or fullyConnectToNextLayer was called before
         * @param modifier for a neural network layer builder
         * @return this builder
         */
        public NeuralNetworkBuilder outputLayer(Consumer<NeuralNetworkLayer.NeuralNetworkLayerBuilder> modifier) {
            modifier = checkAndFullyConnectToPreviousLayer(modifier);

            NeuralNetworkLayer.NeuralNetworkLayerBuilder builder = NeuralNetworkLayer.build();
            modifier.andThen(b -> b.neuralNetwork(this.nn)
                    .layerID(this.nn.layers.size())
                    .layerType(NeuralNetworkLayer.LayerType.OUTPUT))
                    .accept(builder);

            try {
                this.nn.layers.add(builder.finish());
            } catch (NotStrictlyPositiveException e) {
                throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
            }

            return this;
        }

        private Consumer<NeuralNetworkLayer.NeuralNetworkLayerBuilder> checkAndFullyConnectToPreviousLayer(Consumer<NeuralNetworkLayer.NeuralNetworkLayerBuilder> modifier) {
            if (this.nextFullyConnected) {
                modifier = modifier.andThen(b -> b.fullyConnectTo(this.nn.layers.get(this.nn.layers.size() - 1)));
                this.nextFullyConnected = false;
            }
            return modifier;
        }

        /**
         * Fully connects the next layer to the previously build layer. Has no
         * effect if it is called before inputLayer or after outputLayer.
         *
         * @return this builder
         */
        public NeuralNetworkBuilder fullyConnectToNextLayer() {
            this.nextFullyConnected = true;
            return this;
        }

        private NeuralNetwork finish() {
            return this.nn;
        }

    }

    private final List<NeuralNetworkLayer> layers = new ArrayList<>();

    private NeuralNetwork() {}

    public static NeuralNetworkBuilder build() {
        return new NeuralNetworkBuilder();
    }

    public RealVector process(RealVector input) {
        RealVector output = this.layers.get(0).process(input);
        for (int i = 1; i < this.layers.size(); i++) {
            output = this.layers.get(i).process();
        }
        return output;
    }

    public void splitConnection(Connection connection) {
        Neuron start = connection.start();
        Neuron end = connection.end();
        Neuron intermediateNeuron = null;

        int layerDistance = end.getLayerID() - start.getLayerID();

        // initialize intermediateNeuron depending on the positions of start and end
        if (Math.abs(layerDistance) != 1) {
            // Signum ==  0 --> Neurons are in the same layer (lateral connection) --> add new neuron between them
            // Signum ==  1 --> at least one layer between the neurons (forward connection) --> add neuron to first layer after start
            // Signum == -1 --> at least one layer between the neurons (recurrent connection) --> add neuron to first layer before start
            intermediateNeuron = this.layers.get(start.getLayerID() + Integer.signum(layerDistance)).addNewNeuron(0);
            intermediateNeuron.addConnectionFrom(start, connection.getWeight());
        } else {
            // Signum ==  1 --> Neurons are in consecutive layers (forward connection) --> add new layer between them
            // Signum == -1 --> Neurons are in consecutive layers (recurrent connection) --> add new layer between them
            addNewLayerAtPosition(start.getLayerID() + layerDistance, start, connection.getWeight());
        }

        // replace old connection with two new ones --> configure weights in a way that the new connections are equivalent to the old one
        // connection from start to intermediateNeuron was already inserted with the weight of the old connection
        end.addConnectionFrom(intermediateNeuron, 1);
        connection.delete();
    }

    /**
     * Creates a new {@link NeuralNetworkLayer} with one neuron and a connection to it
     * @param position id of the new layer
     * @param connectionStart start neuron of the only connection to this layer
     * @param connectionWeight weight of the connection to this layer
     */
    private void addNewLayerAtPosition(int position, Neuron connectionStart, double connectionWeight) {
        // increase indices of all following layers by one
        this.layers.subList(position, this.layers.size())
                .forEach(layer -> layer.setLayerID(layer.getLayerID() + 1));

        NeuralNetworkLayer newLayer = NeuralNetworkLayer.build()
                .numberOfNeurons(1)
                .layerType(NeuralNetworkLayer.LayerType.HIDDEN)
                .neuralNetwork(this)
                .layerID(position)
                .addConnection(new Connection.ConnectionPrototype(connectionStart.getNeuronID(), new Neuron.NeuronID(position, 0), connectionWeight))
                .finish();

        this.layers.add(position, newLayer);
    }

    public Neuron getNeuron(Neuron.NeuronID id) {
        if (id.layerID() > layers.size()) {
            throw new IllegalArgumentException(String.format("Can't find neuron with id = %s because the neural network only has %d layers", id, this.layers.size()));
        }
        return this.layers.get(id.layerID()).getNeuron(id.neuronID());
    }
}
