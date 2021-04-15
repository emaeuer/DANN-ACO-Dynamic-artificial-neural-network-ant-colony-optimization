package de.emaeuer.ann.impl;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.LayerType;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkBuilder;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;

import java.util.function.Consumer;

import static de.emaeuer.ann.LayerType.*;

public class NeuralNetworkBuilderImpl implements NeuralNetworkBuilder<NeuralNetworkLayerBuilderImpl> {

    private static final String EXCEPTION_MESSAGE_PATTERN = "Failed to create neural network the %s is missing";

    private NeuralNetworkImpl nn = new NeuralNetworkImpl();

    private boolean nextFullyConnected = false;

    private int necessaryModificationFlag = 0b11;

    private ConfigurationHandler<NeuralNetworkConfiguration> configuration = new ConfigurationHandler<>(NeuralNetworkConfiguration.class);

    @Override
    public NeuralNetworkBuilder<NeuralNetworkLayerBuilderImpl> configure(ConfigurationHandler<NeuralNetworkConfiguration> configuration) {
        this.configuration = configuration;
        return this;
    }

    @Override
    public NeuralNetworkBuilderImpl inputLayer() {
        Consumer<NeuralNetworkLayerBuilderImpl> defaultModifier = getDefaultModifierFromConfiguration(NeuralNetworkConfiguration.INPUT_ACTIVATION_FUNCTION);

        // add additional neuron if bias is implicit
        if (!this.nn.usesExplicitBias()) {
            defaultModifier = defaultModifier.andThen(b -> b.numberOfNeurons(configuration.getValue(NeuralNetworkConfiguration.INPUT_LAYER_SIZE, Integer.class) + 1));
        }

        return inputLayer(defaultModifier);
    }

    @Override
    public NeuralNetworkBuilderImpl inputLayer(int size) {
        // add additional neuron if bias is implicit
        return inputLayer(b -> b.numberOfNeurons(size + (this.nn.usesExplicitBias() ? 0 : 1)));
    }

    /**
     * Builds a neural network input layer. Automatically defines and overwrites
     * if present the layer type, the layer id and the neural network of the neural
     * network layer builder.
     *
     * @param modifier for a neural network layer builder
     * @return this builder
     * @throws IllegalStateException if this method was called previously
     */
    @Override
    public NeuralNetworkBuilderImpl inputLayer(Consumer<NeuralNetworkLayerBuilderImpl> modifier) {
        if (0 == ((this.necessaryModificationFlag >> 1) & 1)) {
            throw new IllegalStateException("Input layer was already set and can't be overridden");
        }

        NeuralNetworkLayerBuilderImpl builder = configureLayer(modifier, INPUT);

        try {
            this.nn.getLayers().add(builder.finish());
        } catch (NotStrictlyPositiveException e) {
            throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
        }

        this.necessaryModificationFlag &= 0b01;

        return this;
    }

    @Override
    public NeuralNetworkBuilderImpl hiddenLayer(int size) {
        Consumer<NeuralNetworkLayerBuilderImpl> defaultModifier = getDefaultModifierFromConfiguration(NeuralNetworkConfiguration.HIDDEN_ACTIVATION_FUNCTION);
        return hiddenLayer(defaultModifier.andThen(b -> b.numberOfNeurons(size)));
    }

    /**
     * Builds a neural network hidden layer. Automatically defines and overwrites
     * if present the layer type, the layer id and the neural network of the neural
     * network layer builder.
     *
     * @param modifier for a neural network layer builder
     * @return this builder
     * @throws IllegalStateException if this method was called before inputLayer or after outputLayer
     */
    @Override
    public NeuralNetworkBuilderImpl hiddenLayer(Consumer<NeuralNetworkLayerBuilderImpl> modifier) {
        if (1 == ((this.necessaryModificationFlag >> 1) & 1)) {
            throw new IllegalStateException("Can't add hidden layer before the input layer");
        } else if (0 == (this.necessaryModificationFlag & 1)) {
            throw new IllegalStateException("Can't add hidden layer after the output layer");
        }

        Consumer<NeuralNetworkLayerBuilderImpl> modifierFromConfig = b -> b
                .activationFunction(ActivationFunction.valueOf(configuration.getValue(NeuralNetworkConfiguration.HIDDEN_ACTIVATION_FUNCTION, String.class)))
                .maxWeight(configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MAX, Double.class))
                .minWeight(configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MIN, Double.class));
        // apply configuration modifier first because external modifier should overwrite values if set
        modifier = modifierFromConfig.andThen(modifier);

        NeuralNetworkLayerBuilderImpl builder = configureLayer(modifier, HIDDEN);

        try {
            this.nn.getLayers().add(builder.finish());
        } catch (NotStrictlyPositiveException e) {
            throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
        }

        return this;
    }

    @Override
    public NeuralNetworkBuilder<NeuralNetworkLayerBuilderImpl> outputLayer() {
        Consumer<NeuralNetworkLayerBuilderImpl> defaultModifier = getDefaultModifierFromConfiguration(NeuralNetworkConfiguration.OUTPUT_ACTIVATION_FUNCTION);
        return outputLayer(defaultModifier.andThen(b -> b.numberOfNeurons(configuration.getValue(NeuralNetworkConfiguration.OUTPUT_LAYER_SIZE, Integer.class))));
    }

    @Override
    public NeuralNetworkBuilderImpl outputLayer(int size) {
        return outputLayer(b -> b.numberOfNeurons(size));
    }

    /**
     * Builds a neural network output layer. Automatically defines and overwrites
     * if present the layer type, the layer id and the neural network of the neural
     * network layer builder.
     *
     * @param modifier for a neural network layer builder
     * @return this builder
     * @throws IllegalStateException    if this method was called previously or before inputLayer
     * @throws IllegalArgumentException if the modifier doesn't contain connection definitions or fullyConnectToNextLayer was called before
     */
    @Override
    public NeuralNetworkBuilderImpl outputLayer(Consumer<NeuralNetworkLayerBuilderImpl> modifier) {
        if (0 == (this.necessaryModificationFlag & 1)) {
            throw new IllegalStateException("Output layer was already set and can't be overridden");
        } else if (1 == ((this.necessaryModificationFlag >> 1) & 1)) {
            throw new IllegalStateException("Can't add output layer before the input layer");
        }

        NeuralNetworkLayerBuilderImpl builder = configureLayer(modifier, OUTPUT);

        try {
            this.nn.getLayers().add(builder.finish());
        } catch (NotStrictlyPositiveException e) {
            throw new IllegalArgumentException("Failed to create layer because no connections to this layer were defined", e);
        }

        this.necessaryModificationFlag &= 0b10;

        return this;
    }

    private NeuralNetworkLayerBuilderImpl configureLayer(Consumer<NeuralNetworkLayerBuilderImpl> modifier, LayerType p) {
        // set default activation function may be overridden by modifier
        NeuralNetworkLayerBuilderImpl builder = NeuralNetworkLayerImpl.build();

        // neural network has to be set before and after because settings in modifier may need the neural network
        // and second setting guarantees that the wright neural network was set
        Consumer<NeuralNetworkLayerBuilderImpl> basicModifier = b -> b.neuralNetwork(this.nn);
        modifier = basicModifier.andThen(modifier);
        modifier = modifier.andThen(b -> b.neuralNetwork(this.nn)
                .layerID(this.nn.getLayers().size())
                .layerType(p));
        modifier = checkAndFullyConnectToPreviousLayer(modifier);
        modifier.accept(builder);
        return builder;
    }

    private Consumer<NeuralNetworkLayerBuilderImpl> getDefaultModifierFromConfiguration(NeuralNetworkConfiguration activationFunction) {
        return b -> b
                .activationFunction(ActivationFunction.valueOf(configuration.getValue(activationFunction, String.class)))
                .maxWeight(configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MAX, Double.class))
                .minWeight(configuration.getValue(NeuralNetworkConfiguration.WEIGHT_MIN, Double.class));
    }

    private Consumer<NeuralNetworkLayerBuilderImpl> checkAndFullyConnectToPreviousLayer(Consumer<NeuralNetworkLayerBuilderImpl> modifier) {
        if (this.nn.getDepth() == 0) {
            // fully connected isn't possible for input layer
            this.nextFullyConnected = false;
        } else if (this.nextFullyConnected) {
            modifier = modifier.andThen(b -> b.fullyConnectTo(this.nn.getLayers().get(this.nn.getLayers().size() - 1).getNeurons()));
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
    @Override
    public NeuralNetworkBuilderImpl fullyConnectToNextLayer() {
        this.nextFullyConnected = true;
        return this;
    }

    @Override
    public NeuralNetworkBuilderImpl implicitBias() {
        this.nn.setUsesExplicitBias(false);
        return this;
    }

    @Override
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
