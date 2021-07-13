package de.emaeuer.environment.bird.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.BooleanConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;

public enum FlappyBirdConfiguration implements DefaultConfiguration<FlappyBirdConfiguration> {
    GAP_SIZE("Gap size", new IntegerConfigurationValue(300, 150, 600)),
    PIPE_WIDTH("Width of pipes", new IntegerConfigurationValue(100, 20, 300)),
    PIPE_DISTANCE("Distance of pipes", new IntegerConfigurationValue(400, 100, 600)),
    HEIGHT_INPUT("Height is used as input", new BooleanConfigurationValue(true)),
    VELOCITY_INPUT("Velocity is used as input", new BooleanConfigurationValue(true)),
    DISTANCE_INPUT("Distance to next pipe is used as input", new BooleanConfigurationValue(true)),
    GAP_INPUT("Next gap height is used as input", new BooleanConfigurationValue(true));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    FlappyBirdConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
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
