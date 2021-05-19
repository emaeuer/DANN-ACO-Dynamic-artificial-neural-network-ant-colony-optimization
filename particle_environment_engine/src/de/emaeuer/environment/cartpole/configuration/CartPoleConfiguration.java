package de.emaeuer.environment.cartpole.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.DoubleConfigurationValue;

public enum CartPoleConfiguration implements DefaultConfiguration<CartPoleConfiguration> {
    GRAVITY("Gravity", new DoubleConfigurationValue(9.8)),
    CART_MASS("Cart mass", new DoubleConfigurationValue(1)),
    POLE_MASS("Cart mass", new DoubleConfigurationValue(0.1)),
    POLE_LENGTH("Length of the pole", new DoubleConfigurationValue(1)),
    STEERING_FORCE("Amplitude of the steering force", new DoubleConfigurationValue(10)),
    TAU("Time between updates", new DoubleConfigurationValue(0.02)),
    THETA_THRESHOLD("Threshold for the pole angle (Radians)", new DoubleConfigurationValue(12 * 2 * Math.PI / 360)),
    X_THRESHOLD("Threshold for the cart position", new DoubleConfigurationValue(2.4));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    CartPoleConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
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
