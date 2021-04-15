package de.emaeuer.optimization.configuration;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;

public enum OptimizationState implements StateParameter<OptimizationState> {
    CURRENT_RUN("Number of runs", NumberStateValue.class),
    CURRENT_ITERATION("Number of iterations", NumberStateValue.class),
    FITNESS_VALUE("Best fitness value", NumberStateValue.class),
    ITERATION_DISTRIBUTION("Distribution of number of evaluations per run", DistributionStateValue.class),
    FITNESS_DISTRIBUTION("Distribution of run fitness", DistributionStateValue.class),
    HIDDEN_NODES_DISTRIBUTION("Distribution of number of hidden nodes", DistributionStateValue.class),
    CONNECTIONS_DISTRIBUTION("Distribution of number of connections", DistributionStateValue.class),
    AVERAGE_RUN_FITNESS_SERIES("Average fitness over all runs", DataSeriesStateValue.class),
    FITNESS_SERIES("Fitness of current iteration", DataSeriesStateValue.class),
    IMPLEMENTATION_STATE("State of the optimization method", EmbeddedState.class),
    BEST_SOLUTION("Currently best solution", GraphStateValue.class);

    private final String name;
    private final Class<? extends AbstractStateValue<?, ?>> type;

    OptimizationState(String name, Class<? extends AbstractStateValue<?, ?>> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<? extends AbstractStateValue<?, ?>> getExpectedValueType() {
        return this.type;
    }

    @Override
    public String getKeyName() {
        return name();
    }
}
