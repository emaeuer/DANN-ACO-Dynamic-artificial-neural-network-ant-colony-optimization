package de.emaeuer.optimization.dannaco.population;

import java.util.Arrays;

public enum PopulationUpdateStrategies {
    AGE,
    FITNESS,
    PROBABILITY,
    AGE_PROBABILITY,
    SIMILARITY,
    GROUP_BASED;

    public static String[] getNames() {
        return Arrays.stream(PopulationUpdateStrategies.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
