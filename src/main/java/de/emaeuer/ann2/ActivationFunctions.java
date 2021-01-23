package de.emaeuer.ann2;

import java.util.function.DoubleFunction;

public interface ActivationFunctions {

    public static final DoubleFunction<Double> LINEAR_UNTIL_SATURATION = v -> Math.max(Math.min(v, 1), 0);

}
