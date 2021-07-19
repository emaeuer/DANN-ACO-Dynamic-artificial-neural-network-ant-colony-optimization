package de.emaeuer.environment.balance.onedim.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.BooleanConfigurationValue;
import de.emaeuer.configuration.value.DoubleConfigurationValue;

public enum CartPoleConfiguration implements DefaultConfiguration<CartPoleConfiguration> {
    GRAVITY("Gravity", new DoubleConfigurationValue(-9.8)),
    CART_MASS("Cart mass", new DoubleConfigurationValue(1)),
    FORCE_MAGNITUDE("Force magnitude", new DoubleConfigurationValue(10)),
    BINARY_FORCE("Set the network output to -1 or 1", new BooleanConfigurationValue(false)),
    TIME_DELTA("Time delta", new DoubleConfigurationValue(0.01)),
    TRACK_LENGTH("Track length", new DoubleConfigurationValue(4.8)),
    VELOCITY_INPUT("Use velocities as input", new BooleanConfigurationValue(true)),
    POSITION_INPUT("Use position as input", new BooleanConfigurationValue(true)),
    POLE_ONE_LENGTH("Length of pole one", new DoubleConfigurationValue(0.5)),
    POLE_ONE_MASS("Mass of pole one", new DoubleConfigurationValue(0.1)),
    POLE_ONE_ANGLE("Start angle of pole one", new DoubleConfigurationValue(Math.PI / 180)),
    POLE_TWO_LENGTH("Length of pole two", new DoubleConfigurationValue(0.05)),
    POLE_TWO_MASS("Mass of pole two", new DoubleConfigurationValue(0.01)),
    POLE_TWO_ANGLE("Start angle of pole two", new DoubleConfigurationValue(0)),
    RANDOM_START_ANGLE("Start with random pole angle", new BooleanConfigurationValue(false)),
    ANGLE_THRESHOLD("Threshold for the pole angle (Radians)", new DoubleConfigurationValue(Math.PI / 5)),
    PIVOT_FRICTION("Friction of the pivot", new DoubleConfigurationValue(0.000002)),
    USE_TWO_POLES("Cart has tow poles (true) or one pole (false)", new BooleanConfigurationValue(false)),
    PENALIZE_OSCILLATION("Penalize oscillations", new BooleanConfigurationValue(false));

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
