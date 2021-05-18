package de.emaeuer.ann;

import java.util.Arrays;
import java.util.function.DoubleFunction;

public enum ActivationFunction implements DoubleFunction<Double> {

    LINEAR_UNTIL_SATURATION(v -> Math.max(Math.min(v, 1), 0)),
    IDENTITY(v -> v),
    RELU(v -> Math.max(0, v)),
    SIGMOID(v -> 1 / (1 + Math.exp(-v))),
    TANH(Math::tanh);

    private final DoubleFunction<Double> activationFunction;

    private ActivationFunction(DoubleFunction<Double> activationFunction) {
        this.activationFunction = activationFunction;
    }

    public static String[] getNames() {
        return Arrays.stream(ActivationFunction.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }

    public DoubleFunction<Double> getActivationFunction() {
        return activationFunction;
    }

    @Override
    public Double apply(double value) {
        return this.activationFunction.apply(value);
    }

}
