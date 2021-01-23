package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.impl.NeuralNetworkImpl;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;

import java.util.function.Consumer;

import static de.emaeuer.ann.NeuralNetworkLayer.LayerType.*;

public class NeuralNetworkBuilder {

    private static final String EXCEPTION_MESSAGE_PATTERN = "Failed to create neural network the %s is missing";

    private NeuralNetworkImpl nn = new NeuralNetworkImpl();

    private boolean nextFullyConnected = false;

    private int necessaryModificationFlag = 0b11;

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
    public NeuralNetworkBuilder inputLayer(Consumer<NeuralNetworkLayerBuilder> modifier) {
        if (0 == ((this.necessaryModificationFlag >> 1) & 1)) {
            throw new IllegalStateException("Input layer was already set and can't be overridden");
        }

        NeuralNetworkLayerBuilder builder = configureLayer(modifier, INPUT);

        try {
            this.nn.getLayers().add(builder.finish());
        } catch (NotStrictlyPositiveException e) {
            throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
        }

        this.necessaryModificationFlag &= 0b01;

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
    public NeuralNetworkBuilder hiddenLayer(Consumer<NeuralNetworkLayerBuilder> modifier) {
        if (1 == ((this.necessaryModificationFlag >> 1) & 1)) {
            throw new IllegalStateException("Can't add hidden layer before the input layer");
        } else if (0 == (this.necessaryModificationFlag & 1)) {
            throw new IllegalStateException("Can't add hidden layer after the output layer");
        }

        NeuralNetworkLayerBuilder builder = configureLayer(modifier, HIDDEN);

        try {
            this.nn.getLayers().add(builder.finish());
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
    public NeuralNetworkBuilder outputLayer(Consumer<NeuralNetworkLayerBuilder> modifier) {
        if (0 == (this.necessaryModificationFlag & 1)) {
            throw new IllegalStateException("Output layer was already set and can't be overridden");
        } else if (1 == ((this.necessaryModificationFlag >> 1) & 1)) {
            throw new IllegalStateException("Can't add output layer before the input layer");
        }


        NeuralNetworkLayerBuilder builder = configureLayer(modifier, OUTPUT);

        try {
            this.nn.getLayers().add(builder.finish());
        } catch (NotStrictlyPositiveException e) {
            throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
        }

        this.necessaryModificationFlag &= 0b10;

        return this;
    }

    private NeuralNetworkLayerBuilder configureLayer(Consumer<NeuralNetworkLayerBuilder> modifier, NeuralNetworkLayer.LayerType p) {
        NeuralNetworkLayerBuilder builder = NeuralNetworkLayer.build();
        modifier = modifier.andThen(b -> b.neuralNetwork(this.nn)
                .layerID(this.nn.getLayers().size())
                .layerType(p));
        modifier = checkAndFullyConnectToPreviousLayer(modifier);
        modifier.accept(builder);
        return builder;
    }

    private Consumer<NeuralNetworkLayerBuilder> checkAndFullyConnectToPreviousLayer(Consumer<NeuralNetworkLayerBuilder> modifier) {
        if (this.nn.getDepth() == 0) {
            // fully connected isn't possible for input layer
            this.nextFullyConnected = false;
        } else if (this.nextFullyConnected) {
            modifier = modifier.andThen(b -> b.fullyConnectTo(this.nn.getLayers().get(this.nn.getLayers().size() - 1)));
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

    public NeuralNetwork finish() {
        if (this.necessaryModificationFlag != 0) {
            throw new IllegalStateException(buildMessageForCurrentModificationFlag());
        }

        // disable further modification
        NeuralNetwork finishedNetwork = this.nn;
        this.nn = null;

        return finishedNetwork;
    }

    private String buildMessageForCurrentModificationFlag() {
        int firstMissingArgument = Integer.highestOneBit(this.necessaryModificationFlag);

        return switch (firstMissingArgument) {
            case 0 -> String.format(EXCEPTION_MESSAGE_PATTERN, "output layer");
            case 1 -> String.format(EXCEPTION_MESSAGE_PATTERN, "input layer");
            default -> throw new IllegalStateException("Unexpected value: " + firstMissingArgument);
        };
    }

}
