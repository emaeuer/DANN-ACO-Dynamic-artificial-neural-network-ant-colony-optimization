package de.emaeuer.optimization;

import java.util.Arrays;

public enum OptimizationMethodNames {
    NEAT,
    PACO;

    public static String[] getNames() {
        return Arrays.stream(OptimizationMethodNames.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
