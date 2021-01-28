package de.emaeuer.ann;

import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;
import java.util.function.DoubleFunction;

public interface NeuralNetworkLayerBuilder {

    NeuralNetworkLayerBuilder activationFunction(DoubleFunction<Double> function);

    NeuralNetworkLayerBuilder numberOfNeurons(int number);

    NeuralNetworkLayerBuilder addConnection(NeuronID start, NeuronID end, double weight);

    NeuralNetworkLayerBuilder fullyConnectTo(List<NeuronID> otherNeurons);

    NeuralNetworkLayerBuilder bias(RealVector bias);

}
