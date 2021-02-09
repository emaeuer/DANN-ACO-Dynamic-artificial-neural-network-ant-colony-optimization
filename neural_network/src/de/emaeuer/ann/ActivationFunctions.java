package de.emaeuer.ann;

import java.util.function.DoubleFunction;

public interface ActivationFunctions {

    public static final DoubleFunction<Double> LINEAR_UNTIL_SATURATION = v -> Math.max(Math.min(v, 1), 0);

    public static final DoubleFunction<Double> IDENTITY = v -> v;

    public static final DoubleFunction<Double> RELU = v -> Math.max(0, v);

}