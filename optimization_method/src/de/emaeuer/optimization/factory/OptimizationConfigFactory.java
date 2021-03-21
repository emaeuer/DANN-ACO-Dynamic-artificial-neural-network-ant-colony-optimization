package de.emaeuer.optimization.factory;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.aco.configuration.AcoConfiguration;
import de.emaeuer.optimization.neat.configuration.NeatConfiguration;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

public class OptimizationConfigFactory {

    private OptimizationConfigFactory() {}

    public static ConfigurationHandler<?> createOptimizationConfiguration(OptimizationMethodNames name) {
        return switch (name) {
            case ACO -> new ConfigurationHandler<>(AcoConfiguration.class);
            case NEAT -> new ConfigurationHandler<>(NeatConfiguration.class);
            case PACO -> new ConfigurationHandler<>(PacoConfiguration.class);
        };
    }

}
