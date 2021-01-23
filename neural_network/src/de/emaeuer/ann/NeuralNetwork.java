package de.emaeuer.ann;

import de.emaeuer.ann.util.NeuralNetworkBuilder;
import de.emaeuer.ann.util.NeuralNetworkModifier;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;
import java.util.stream.Stream;

public interface NeuralNetwork extends Iterable<NeuralNetworkLayer> {

    public static NeuralNetworkBuilder build() {
        return new NeuralNetworkBuilder();
    }

    public RealVector process(RealVector input);

    public Neuron getNeuron(Neuron.NeuronID key);

    public NeuralNetworkLayer getLayer(int i);

    public NeuralNetworkModifier modify();

    public int getDepth();

    public Stream<NeuralNetworkLayer> stream();

    public List<NeuralNetworkLayer> getLayers();
}
