package de.emaeuer.environment.pong.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;

public enum PongConfiguration implements DefaultConfiguration<PongConfiguration> {
    BALL_RADIUS("Ball radius", new IntegerConfigurationValue(20, 10, 50)),
    BALL_VELOCITY("Ball velocity", new IntegerConfigurationValue(5, 1, 50)),
    BALL_ANGLE("Ball start angle", new IntegerConfigurationValue(180, 0, 359)),
    PADDLE_HEIGHT("Paddle width", new IntegerConfigurationValue(150, 50, 300)),
    PADDLE_VELOCITY("Paddle velocity", new IntegerConfigurationValue(5, 1, 50)),
    BALL_MAX_REFLECTION_ANGLE("Max reflection angle of the ball", new IntegerConfigurationValue(45, 0, 85));


    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    PongConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
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
