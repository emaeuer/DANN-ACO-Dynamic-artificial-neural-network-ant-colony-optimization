package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;

import java.util.function.BiConsumer;

public enum EnvironmentConfiguration implements DefaultConfiguration<EnvironmentConfiguration> {
    SEED("Seed for generating the environment", new IntegerConfigurationValue(9369319)),
    MAX_STEP_NUMBER("Step number threshold", new DoubleConfigurationValue(10000, 1, Double.MAX_VALUE)),
    MAX_FITNESS_SCORE("Fitness threshold", new DoubleConfigurationValue(10000, 0, Double.MAX_VALUE)),
    ENVIRONMENT_IMPLEMENTATION("The configuration of the selected environment implementation", new EmbeddedConfiguration<>(EnvironmentConfigurationFactory.createEnvironmentConfiguration(EnvironmentImplementations.FLAPPY_BIRD))),
    ENVIRONMENT_IMPLEMENTATION_NAME("Environment implementation", new StringConfigurationValue("FLAPPY_BIRD", EnvironmentImplementations.getNames()),
            (v, h) -> {
                EnvironmentImplementations implementationName = EnvironmentImplementations.valueOf(v.getStringRepresentation());
                ConfigurationHandler<?> configuration = EnvironmentConfigurationFactory.createEnvironmentConfiguration(implementationName);
                h.setValue(EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION, new EmbeddedConfiguration<>(configuration));
            });

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;
    private final BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<EnvironmentConfiguration>> changeAction;

    EnvironmentConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this(name, defaultValue, null);
    }

    EnvironmentConfiguration(String name, AbstractConfigurationValue<?> defaultValue, BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<EnvironmentConfiguration>> changeAction) {
        this.defaultValue = defaultValue;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) defaultValue.getClass();
        this.name = name;
        this.changeAction = changeAction;
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
    public void executeChangeAction(AbstractConfigurationValue<?> newValue, ConfigurationHandler<EnvironmentConfiguration> handler) {
        if (refreshNecessary()) {
            changeAction.accept(newValue, handler);
        }
    }

    @Override
    public boolean refreshNecessary() {
        return this.changeAction != null;
    }

    @Override
    public String getKeyName() {
        return name();
    }
}
