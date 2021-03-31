package de.emaeuer.optimization;

import java.util.Arrays;

public enum OptimizationMethodNames {
    ACO,
    NEAT,
    PACO,
    PACO_COLONY;

    public static String[] getNames() {
        return Arrays.stream(OptimizationMethodNames.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
