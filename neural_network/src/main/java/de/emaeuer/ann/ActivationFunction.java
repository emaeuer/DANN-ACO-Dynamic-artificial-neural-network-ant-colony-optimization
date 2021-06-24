package de.emaeuer.ann;

import java.util.Arrays;
import java.util.function.DoubleFunction;

public enum ActivationFunction implements DoubleFunction<Double> {

    LINEAR_UNTIL_SATURATION(v -> Math.max(Math.min(v, 1), 0), 0, 1),
    IDENTITY(v -> v, Integer.MIN_VALUE, Integer.MAX_VALUE),
    RELU(v -> Math.max(0, v), 0, Integer.MAX_VALUE),
    SIGMOID(v -> 1 / (1 + Math.exp(-v * 4.9)), 0, 1),
    TANH(Math::tanh, -1 , 1);

    private final DoubleFunction<Double> activationFunction;
    private final double minActivation;
    private final double maxActivation;

    ActivationFunction(DoubleFunction<Double> activationFunction, double minActivation, double maxActivation) {
        this.activationFunction = activationFunction;
        this.minActivation = minActivation;
        this.maxActivation = maxActivation;
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

    public double getMaxActivation() {
        return maxActivation;
    }

    public double getMinActivation() {
        return minActivation;
    }
}
