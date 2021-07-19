package de.emaeuer.environment.configuration;

import java.util.Arrays;

public enum EnvironmentImplementations {
    FLAPPY_BIRD,
    ONE_DIMENSIONAL_CART_POLE,
    TWO_DIMENSIONAL_CART_POLE,
    XOR;

    public static String[] getNames() {
        return Arrays.stream(EnvironmentImplementations.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
