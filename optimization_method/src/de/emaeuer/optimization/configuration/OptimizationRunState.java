package de.emaeuer.optimization.configuration;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;

public enum OptimizationRunState implements StateParameter<OptimizationRunState> {
    RUN_NUMBER("Number of run", NumberStateValue.class, true),
    EVALUATION_NUMBER("Number of evaluations", NumberStateValue.class, true),
    FITNESS_VALUE("Highest fitness value of run", NumberStateValue.class, false),
    USED_HIDDEN_NODES("Used hidden nodes", CollectionDistributionStateValue.class, true),
    USED_CONNECTIONS("Used connections", CollectionDistributionStateValue.class, true),
    FITNESS_VALUES("Fitness values", CollectionDistributionStateValue.class, true),
    CURRENT_BEST_SOLUTION("Currently best solution", GraphStateValue.class, false),
    RUN_FINISHED("Run finished", NumberStateValue.class, true);

    private final String name;
    private final Class<? extends AbstractStateValue<?, ?>> type;
    private final boolean export;

    OptimizationRunState(String name, Class<? extends AbstractStateValue<?, ?>> type, boolean export) {
        this.name = name;
        this.type = type;
        this.export = export;
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
        return export;
    }
}
