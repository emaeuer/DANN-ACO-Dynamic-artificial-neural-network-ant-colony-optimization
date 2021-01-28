package de.emaeuer.ann;

import java.util.function.Consumer;

public interface NeuralNetworkBuilder<T extends NeuralNetworkLayerBuilder> {

    NeuralNetworkBuilder<T> inputLayer(int size);

    NeuralNetworkBuilder<T> inputLayer(Consumer<T> modifier);

    NeuralNetworkBuilder<T> hiddenLayer(int size);

    NeuralNetworkBuilder<T> hiddenLayer(Consumer<T> modifier);

    NeuralNetworkBuilder<T> outputLayer(int size);

    NeuralNetworkBuilder<T> outputLayer(Consumer<T> modifier);

    NeuralNetworkBuilder<T> fullyConnectToNextLayer();

    NeuralNetwork finish();
}
