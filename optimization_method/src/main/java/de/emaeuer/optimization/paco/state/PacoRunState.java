package de.emaeuer.optimization.paco.state;

import de.emaeuer.configuration.value.DoubleConfigurationValue;
import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.*;

public enum PacoRunState implements StateParameter<PacoRunState> {
    CONNECTION_WEIGHTS_SCATTERED("Weight distribution in population", MapOfStateValue.class),
    USED_GROUPS("Groups of population", CumulatedDataSeriesStateValue.class);

    private final String name;
    private final Class<? extends AbstractStateValue<?, ?>> type;

    PacoRunState(String name, Class<? extends AbstractStateValue<?,?>> type) {
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
