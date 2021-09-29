package de.emaeuer.optimization.dannaco.state;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;

public enum DannacoRunState implements StateParameter<DannacoRunState> {
    CONNECTION_WEIGHTS_SCATTERED("Weight distribution in population", MapOfStateValue.class),
    USED_GROUPS("Groups of population", CumulatedDataSeriesStateValue.class);

    private final String name;
    private final Class<? extends AbstractStateValue<?, ?>> type;

    DannacoRunState(String name, Class<? extends AbstractStateValue<?,?>> type) {
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
        return true;
    }
}
