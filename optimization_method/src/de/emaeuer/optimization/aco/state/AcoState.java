package de.emaeuer.optimization.aco.state;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.DataSeriesStateValue;
import de.emaeuer.state.value.MapOfStateValue;

public enum AcoState implements StateParameter<AcoState> {
    FITNESS_OF_ALL_COLONIES("Fitness of all colonies", DataSeriesStateValue.class),
    COLONY_FITNESS("Fitness of each colony", MapOfStateValue.class);

    private final String name;
    private final Class<? extends AbstractStateValue<?, ?>> type;

    AcoState(String name, Class<? extends AbstractStateValue<?,?>> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<? extends AbstractStateValue<?, ?>> getExpectedValueType() {
        return type;
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
