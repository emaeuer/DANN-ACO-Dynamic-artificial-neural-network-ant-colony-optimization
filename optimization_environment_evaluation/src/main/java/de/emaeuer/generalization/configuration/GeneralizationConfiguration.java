package de.emaeuer.generalization.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;

public enum GeneralizationConfiguration implements DefaultConfiguration<GeneralizationConfiguration> {
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
