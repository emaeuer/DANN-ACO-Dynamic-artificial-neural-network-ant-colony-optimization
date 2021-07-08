package de.emaeuer.environment.xor;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;
import de.emaeuer.environment.configuration.GeneralizationConfiguration;

public enum XORGeneralizationConfiguration implements DefaultConfiguration<XORGeneralizationConfiguration>, GeneralizationConfiguration<XORGeneralizationConfiguration> {
    GENERALIZATION_DATA_SET_SIZE("Number of random input pairs", new IntegerConfigurationValue(50, 1, 200));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    XORGeneralizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this.defaultValue = defaultValue;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) defaultValue.getClass();
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public AbstractConfigurationValue<?> getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Class<?> getValueType() {
        return this.type;
    }

    @Override
    public boolean refreshNecessary() {
        return false;
    }

    @Override
    public String getKeyName() {
        return name();
    }

}
