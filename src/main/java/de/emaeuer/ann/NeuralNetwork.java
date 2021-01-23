package de.emaeuer.ann;

import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class NeuralNetwork implements Iterable<NeuralNetworkLayer>, Cloneable {

    private final List<NeuralNetworkLayer> layers = new ArrayList<>();

    private final NeuralNetworkModifier modifier;

    public NeuralNetwork(int... neuronNumberOfLayers) {
        if (neuronNumberOfLayers.length < 2) {
            throw new IllegalArgumentException("Neural network needs at least two layers given were " + neuronNumberOfLayers.length);
        }

        // create input layer
        NeuralNetworkLayer previousLayer = new NeuralNetworkLayer(neuronNumberOfLayers[0], 0);
        for (int i = 1; i < neuronNumberOfLayers.length; i++) {
            this.layers.add(previousLayer);
            // build sequence of linked layers
            previousLayer = new NeuralNetworkLayer(neuronNumberOfLayers[i], previousLayer, i);
        }
        this.layers.add(previousLayer);

        this.modifier = new NeuralNetworkModifier(this);
    }

    public RealVector process(RealVector input) {
        return getInputLayer().processInput(input);
    }

    public NeuralNetworkLayer getInputLayer() {
        return this.layers.get(0);
    }

    public int getNumberOfLayers() {
        return this.layers.size();
    }

    public NeuralNetwork copy() {
        int[] layerSizes = this.layers.stream()
                .mapToInt(NeuralNetworkLayer::getNumberOfNeurons)
                .toArray();

        NeuralNetwork copy = new NeuralNetwork(layerSizes);

        for (int i = 1; i < this.layers.size(); i++) {
            copy.getModifier()
                    .modifyLayer(i)
                    .setWeightsOfLayer(this.layers.get(i).getWeights())
                    .setBiasOfLayer(this.layers.get(i).getBias());
        }

        return copy;
    }

    public void randomize() {
        // randomize weights and bias
        stream()
                .flatMap(NeuralNetworkLayer::stream)
                .peek(neuron -> {
                    if (neuron.getIdentifier().layerID() != 0) {
                        neuron.setBias(Math.random() * 2 - 1);
                    }
                })
                .flatMap(Neuron::stream)
                .forEach(connection -> connection.setWeight(Math.random() * 2 - 1));
    }

    public NeuralNetworkLayer getLayer(int id) {
        return this.layers.get(id);
    }

    public Neuron getNeuron(Neuron.NeuronID identifier) {
        return getLayer(identifier.layerID()).getNeuron(identifier.neuronID());
    }

    public Stream<NeuralNetworkLayer> stream() {
        return this.layers.stream();
    }

    public void forEach(Consumer<? super NeuralNetworkLayer> consumer) {
        this.layers.forEach(consumer);
    }

    @Override
    public Iterator<NeuralNetworkLayer> iterator() {
        return this.layers.iterator();
    }

    @Override
    public Spliterator<NeuralNetworkLayer> spliterator() {
        return this.layers.spliterator();
    }

    public NeuralNetworkModifier getModifier() {
        return this.modifier;
    }
}
