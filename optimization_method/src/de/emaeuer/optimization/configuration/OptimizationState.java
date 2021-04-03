package de.emaeuer.optimization.configuration;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;

public enum OptimizationState implements StateParameter<OptimizationState> {
    CURRENT_RUN("Number of runs", NumberStateValue.class),
    CURRENT_ITERATION("Number of iterations", NumberStateValue.class),
    FITNESS_VALUE("Best fitness value", NumberStateValue.class),
    AVERAGE_ITERATIONS("Average number of evaluations per run", NumberStateValue.class),
    AVERAGE_FITNESS("Average run fitness", NumberStateValue.class),
    AVERAGE_HIDDEN_NODES("Average number of hidden nodes", NumberStateValue.class),
    AVERAGE_CONNECTIONS("Average number of connections", NumberStateValue.class),
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
