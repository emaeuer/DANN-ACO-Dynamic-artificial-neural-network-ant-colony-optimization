package de.emaeuer.optimization.dannaco.state;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.DataQuantityStateValue;
import de.emaeuer.state.value.DistributionStateValue;

public enum DannacoState implements StateParameter<DannacoState> {
    MODIFICATION_DISTRIBUTION("Modification distribution", DataQuantityStateValue.class),
    AVERAGE_STANDARD_DEVIATION("Average standard deviation", DistributionStateValue.class);

    private final String name;
    private final Class<? extends AbstractStateValue<?, ?>> type;

    DannacoState(String name, Class<? extends AbstractStateValue<?,?>> type) {
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
