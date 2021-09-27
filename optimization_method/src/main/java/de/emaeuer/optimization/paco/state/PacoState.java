package de.emaeuer.optimization.paco.state;

import de.emaeuer.state.StateParameter;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.DataQuantityStateValue;
import de.emaeuer.state.value.DistributionStateValue;
import de.emaeuer.state.value.NumberStateValue;

public enum PacoState  implements StateParameter<PacoState> {
    MODIFICATION_DISTRIBUTION("Modification distribution", DataQuantityStateValue.class),
    AVERAGE_STANDARD_DEVIATION("Average standard deviation", DistributionStateValue.class);

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
        return true;
    }
}
