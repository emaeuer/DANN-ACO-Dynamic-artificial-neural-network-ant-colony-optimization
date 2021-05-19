package de.emaeuer.configuration;

import de.emaeuer.configuration.value.AbstractConfigurationValue;

import java.io.Serializable;

public interface DefaultConfiguration<T extends Enum<T> & DefaultConfiguration<T>> {

    String getName();

    AbstractConfigurationValue<?> getDefaultValue();

    Class<?> getValueType();

    boolean refreshNecessary();

    String getKeyName();

    default void executeChangeAction(AbstractConfigurationValue<?> newValue, ConfigurationHandler<T> handler) {
        // left empty intentionally
    }

    default boolean isDisabled() {
        return false;
    }
}
