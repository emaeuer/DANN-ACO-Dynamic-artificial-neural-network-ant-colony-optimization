package de.emaeuer.environment.xor;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.BooleanConfigurationValue;
import de.emaeuer.configuration.value.DoubleConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;

public enum XORConfiguration implements DefaultConfiguration<XORConfiguration> {
    TARGET_RANGE("Target range (smaller errors are considered as 0)", new DoubleConfigurationValue(0.2, 0, 1)),
    SHUFFLE_DATA_SET("Shuffle the data set", new BooleanConfigurationValue(true)),
    DATA_SET_SIZE_FACTOR("Number of evaluations of each input", new IntegerConfigurationValue(1, 1, 100));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    XORConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
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
