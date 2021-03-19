package de.emaeuer.optimization.configuration;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;

public enum OptimizationState implements StateParameter<OptimizationState> {
    ITERATION("Number of iterations", NumberStateValue.class),
    FITNESS("Fitness of current iteration", DataSeriesStateValue.class),
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
