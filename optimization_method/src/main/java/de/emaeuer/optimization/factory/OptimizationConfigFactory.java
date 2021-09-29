package de.emaeuer.optimization.factory;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.neat.configuration.NeatConfiguration;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;

public class OptimizationConfigFactory {

    private OptimizationConfigFactory() {
    }

    public static ConfigurationHandler<?> createOptimizationConfiguration(OptimizationMethodNames name) {
        return switch (name) {
            case NEAT -> new ConfigurationHandler<>(NeatConfiguration.class, "NEAT");
            case DANN_ACO -> new ConfigurationHandler<>(DannacoConfiguration.class, "DANN_ACO");
        };
    }

}
