package de.emaeuer.environment.bird.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;

import java.util.function.BiConsumer;

public enum FlappyBirdConfiguration implements DefaultConfiguration<FlappyBirdConfiguration> {
    GAP_SIZE("Gap size", new IntegerConfigurationValue(300, 150, 600)),
    PIPE_WIDTH("Width of pipes", new IntegerConfigurationValue(100, 20, 300)),
    PIPE_DISTANCE("Distance of pipes", new IntegerConfigurationValue(400, 100, 600));

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
