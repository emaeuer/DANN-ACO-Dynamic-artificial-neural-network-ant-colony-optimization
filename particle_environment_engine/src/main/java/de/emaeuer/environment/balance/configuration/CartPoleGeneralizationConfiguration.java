package de.emaeuer.environment.balance.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.NumericListConfigurationValue;

public enum CartPoleGeneralizationConfiguration implements DefaultConfiguration<CartPoleGeneralizationConfiguration> {
    POSITION_START_VALUE("Initial cart position", new NumericListConfigurationValue("0.05,0.25,0.5,0.75,0.95")),
    CART_VELOCITY_START_VALUE("Initial cart velocity", new NumericListConfigurationValue("0.05,0.25,0.5,0.75,0.95")),
    ANGLE_START_VALUE("Initial pole one angle", new NumericListConfigurationValue("0.05,0.25,0.5,0.75,0.95")),
    ANGLE_VELOCITY_START_VALUE("Initial pole one velocity", new NumericListConfigurationValue("0.05,0.25,0.5,0.75,0.95"));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    CartPoleGeneralizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
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
