package de.emaeuer.optimization.paco.population;

import java.util.Arrays;

public enum PopulationUpdateStrategies {
    AGE,
    FITNESS,
    PROBABILITY,
    AGE_PROBABILITY,
    INNOVATION_PROTECTING,
    GROUP_BASED;

    public static String[] getNames() {
        return Arrays.stream(PopulationUpdateStrategies.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
