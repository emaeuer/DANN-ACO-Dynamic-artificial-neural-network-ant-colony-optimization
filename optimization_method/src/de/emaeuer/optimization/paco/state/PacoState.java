package de.emaeuer.optimization.paco.state;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.MapOfStateValue;

public enum PacoState implements StateParameter<PacoState> {
    CONNECTION_WEIGHTS_SCATTERED("Weight distribution in population", MapOfStateValue.class);

    private final String name;
    private final Class<? extends AbstractStateValue<?, ?>> type;

    PacoState(String name, Class<? extends AbstractStateValue<?,?>> type) {
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
