package de.emaeuer.optimization.factory;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.neat.NeatHandler;
import de.emaeuer.optimization.dannaco.DannacoHandler;
import de.emaeuer.state.StateHandler;

public class OptimizationMethodFactory {

    private OptimizationMethodFactory() {}

    public static OptimizationMethod createMethodForConfig(ConfigurationHandler<OptimizationConfiguration> config, StateHandler<OptimizationState> state) {
        OptimizationMethodNames method = OptimizationMethodNames.valueOf(config.getValue(OptimizationConfiguration.METHOD_NAME, String.class));

        return switch (method) {
            case NEAT -> createNEAT(config, state);
            case DANN_ACO -> createDANNACO(config, state);
        };
    }

    private static OptimizationMethod createNEAT(ConfigurationHandler<OptimizationConfiguration> config, StateHandler<OptimizationState> state) {
        return new NeatHandler(config, state);
    }

    private static OptimizationMethod createDANNACO(ConfigurationHandler<OptimizationConfiguration> config, StateHandler<OptimizationState> state) {
        return new DannacoHandler(config, state);
    }
}
