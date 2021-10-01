package de.emaeuer.environment.balance.twodim.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.environment.configuration.GeneralizationConfiguration;

public enum TwoDimensionalCartPoleGeneralizationConfiguration implements DefaultConfiguration<TwoDimensionalCartPoleGeneralizationConfiguration>, GeneralizationConfiguration<TwoDimensionalCartPoleGeneralizationConfiguration> {
    ;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public AbstractConfigurationValue<?> getDefaultValue() {
        return null;
    }

    @Override
    public Class<?> getValueType() {
        return null;
    }

    @Override
    public boolean refreshNecessary() {
        return false;
    }

    @Override
    public String getKeyName() {
        return null;
    }
}
