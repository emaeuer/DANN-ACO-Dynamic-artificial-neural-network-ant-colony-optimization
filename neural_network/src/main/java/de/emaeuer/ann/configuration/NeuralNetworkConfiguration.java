package de.emaeuer.ann.configuration;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.DoubleConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;
import de.emaeuer.configuration.value.StringConfigurationValue;

public enum NeuralNetworkConfiguration implements DefaultConfiguration<NeuralNetworkConfiguration> {
    INPUT_LAYER_SIZE("Number of input neurons", new IntegerConfigurationValue(1, 1, Integer.MAX_VALUE)),
    OUTPUT_LAYER_SIZE("Number of output neurons", new IntegerConfigurationValue(1, 1, Integer.MAX_VALUE)),

    WEIGHT_MAX("Upper bound for connection weights", new DoubleConfigurationValue(5, 1, 500)),
    WEIGHT_MIN("Lower bound for connection weights", new DoubleConfigurationValue(-5, -500, -1)),

    // TODO implement
//    BIAS_AS_ON_NEURON("Realize bias as on neuron", new BooleanConfigurationValue(false)),

    OUTPUT_ACTIVATION_FUNCTION("Output layer activation function", new StringConfigurationValue(ActivationFunction.TANH.name(), ActivationFunction.getNames())),
    INPUT_ACTIVATION_FUNCTION("Input layer activation function", new StringConfigurationValue(ActivationFunction.TANH.name(), ActivationFunction.getNames())),
    HIDDEN_ACTIVATION_FUNCTION("Hidden layer activation function", new StringConfigurationValue(ActivationFunction.TANH.name(), ActivationFunction.getNames()));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;

    NeuralNetworkConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this.defaultValue = defaultValue;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public AbstractConfigurationValue<?> getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Class<?> getValueType() {
        return this.defaultValue.getClass();
    }

    @Override
    public boolean refreshNecessary() {
        return false;
    }

    @Override
    public String getKeyName() {
        return name();
    }

}
