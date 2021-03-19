package de.emaeuer.state;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.state.value.AbstractStateValue;

import java.io.Serializable;

public interface StateParameter<T extends Enum<T> & StateParameter<T>> {

    String getName();

    Class<? extends AbstractStateValue<?, ?>> getExpectedValueType();

    String getKeyName();

}
