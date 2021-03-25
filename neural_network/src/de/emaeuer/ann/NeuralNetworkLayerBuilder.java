package de.emaeuer.ann;

import org.apache.commons.math3.linear.RealVector;

import java.util.List;

public interface NeuralNetworkLayerBuilder {

    NeuralNetworkLayerBuilder activationFunction(ActivationFunction function);

    NeuralNetworkLayerBuilder numberOfNeurons(int number);

    NeuralNetworkLayerBuilder addConnection(NeuronID start, NeuronID end, double weight);

    NeuralNetworkLayerBuilder fullyConnectTo(List<NeuronID> otherNeurons);

    NeuralNetworkLayerBuilder bias(RealVector bias);

    NeuralNetworkLayerBuilder maxWeight(double value);

    NeuralNetworkLayerBuilder minWeight(double value);
}
