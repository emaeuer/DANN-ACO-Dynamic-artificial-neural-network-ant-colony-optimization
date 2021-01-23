package de.emaeuer.ann.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.Neuron;
import de.emaeuer.ann.util.NeuralNetworkModifier;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class NeuralNetworkImpl implements NeuralNetwork {

    private final List<NeuralNetworkLayer> layers = new ArrayList<>();

    private final NeuralNetworkModifier modifier = new NeuralNetworkModifier(this);

    @Override
    public RealVector process(RealVector input) {
        RealVector output = this.layers.get(0).process(input);
        for (int i = 1; i < this.layers.size(); i++) {
            output = this.layers.get(i).process();
        }
        return output;
    }

    @Override
    public Neuron getNeuron(Neuron.NeuronID id) {
        if (id.layerID() > layers.size()) {
            throw new IllegalArgumentException(String.format("Can't find neuron with id = %s because the neural network only has %d layers", id, this.layers.size()));
        }
        return this.layers.get(id.layerID())
                .getNeuron(id.neuronID());
    }

    @Override
    public NeuralNetworkLayer getLayer(int i) {
        return this.layers.get(i);
    }

    @Override
    public NeuralNetworkModifier modify() {
        return this.modifier;
    }

    @Override
    public int getDepth() {
        return this.layers.size();
    }

    @Override
    public List<NeuralNetworkLayer> getLayers() {
        return this.layers;
    }

    @Override
    public Iterator<NeuralNetworkLayer> iterator() {
        return this.layers.iterator();
    }

    @Override
    public void forEach(Consumer<? super NeuralNetworkLayer> action) {
        this.layers.forEach(action);
    }

    @Override
    public Spliterator<NeuralNetworkLayer> spliterator() {
        return this.layers.spliterator();
    }

    @Override
    public Stream<NeuralNetworkLayer> stream() {
        return this.layers.stream();
    }
}
