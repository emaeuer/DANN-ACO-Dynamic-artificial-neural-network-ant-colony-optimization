package de.emaeuer.state;

import de.emaeuer.state.value.AbstractStateValue;

public interface StateParameter<T extends Enum<T> & StateParameter<T>> {

    String getName();

    Class<? extends AbstractStateValue<?, ?>> getExpectedValueType();

    String getKeyName();

    boolean export();

}
