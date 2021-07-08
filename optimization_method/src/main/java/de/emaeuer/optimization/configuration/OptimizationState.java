package de.emaeuer.optimization.configuration;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;

public enum OptimizationState implements StateParameter<OptimizationState> {
    EVALUATION_DISTRIBUTION("Distribution of number of evaluations per run", DistributionStateValue.class),
    FITNESS_DISTRIBUTION("Distribution of run fitness", DistributionStateValue.class),
    HIDDEN_NODES_DISTRIBUTION("Distribution of number of hidden nodes", DistributionStateValue.class),
    CONNECTIONS_DISTRIBUTION("Distribution of number of connections", DistributionStateValue.class),
    FINISHED_RUN_DISTRIBUTION("Distribution of number of finished runs", DistributionStateValue.class),
    AVERAGE_GENERALIZATION_CAPABILITY("Average generalization capability over all runs", DistributionStateValue.class),
    AVERAGE_RUN_FITNESS_SERIES("Average fitness over all runs", DataSeriesStateValue.class),
    GLOBAL_BEST_SOLUTION("Best solution found", GraphStateValue.class),
    STATE_OF_CURRENT_RUN("State of the current run", EmbeddedState.class),
    IMPLEMENTATION_STATE("State of the optimization method", EmbeddedState.class);

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

    @Override
    public boolean export() {
        return false;
    }
}
