package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;

import java.util.function.BiConsumer;

public enum EnvironmentConfiguration implements DefaultConfiguration<EnvironmentConfiguration> {
    SEED("Seed for generating the environment", new IntegerConfigurationValue(9369319), true),
    MAX_STEP_NUMBER("Step number threshold", new DoubleConfigurationValue(10000, 1, Double.MAX_VALUE)),
    MAX_FITNESS_SCORE("Fitness threshold", new DoubleConfigurationValue(10000, 0, Double.MAX_VALUE), true),
    GENERALIZATION_MAX_STEP_NUMBER("Step number threshold for generalization", new DoubleConfigurationValue(10000, 1, Double.MAX_VALUE)),
    GENERALIZATION_MAX_FITNESS_SCORE("Fitness threshold for generalization", new DoubleConfigurationValue(10000, 0, Double.MAX_VALUE), true),
    GENERALIZATION_IMPLEMENTATION("Generalization configuration of the selected environment", new EmbeddedConfiguration<>(GeneralizationConfigurationFactory.createConfiguration(EnvironmentImplementations.FLAPPY_BIRD))),
    TEST_GENERALIZATION("Test the generalization capability", new BooleanConfigurationValue(false), true,
            (v, h) -> {
                boolean value = Boolean.parseBoolean(v.getStringRepresentation());
                h.disableConfiguration(GENERALIZATION_MAX_STEP_NUMBER, !value);
                if (!value) {
                    h.setValue(GENERALIZATION_IMPLEMENTATION, null);
                } else {
                    // must use string instead of enum constant --> pay attention when renaming
                    String environmentName = h.getValue("ENVIRONMENT_IMPLEMENTATION_NAME", String.class);
                    EnvironmentImplementations environment = EnvironmentImplementations.valueOf(environmentName);
                    ConfigurationHandler<?> configuration = GeneralizationConfigurationFactory.createConfiguration(environment);
                    h.setValue(GENERALIZATION_IMPLEMENTATION, new EmbeddedConfiguration<>(configuration));
                }
            }),
    ENVIRONMENT_IMPLEMENTATION("The configuration of the selected environment implementation", new EmbeddedConfiguration<>(EnvironmentConfigurationFactory.createEnvironmentConfiguration(EnvironmentImplementations.FLAPPY_BIRD))),
    ENVIRONMENT_IMPLEMENTATION_NAME("Environment implementation", new StringConfigurationValue("FLAPPY_BIRD", EnvironmentImplementations.getNames()),
            (v, h) -> {
                EnvironmentImplementations implementationName = EnvironmentImplementations.valueOf(v.getStringRepresentation());
                ConfigurationHandler<?> configuration = EnvironmentConfigurationFactory.createEnvironmentConfiguration(implementationName);
                h.setValue(ENVIRONMENT_IMPLEMENTATION, new EmbeddedConfiguration<>(configuration));

                boolean testGeneralization = h.getValue(TEST_GENERALIZATION, Boolean.class);
                if (testGeneralization) {
                    configuration = GeneralizationConfigurationFactory.createConfiguration(implementationName);
                    h.setValue(GENERALIZATION_IMPLEMENTATION, new EmbeddedConfiguration<>(configuration));
                }
            });

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;
    private final BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<EnvironmentConfiguration>> changeAction;
    private final boolean disabled;

    EnvironmentConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this(name, defaultValue, false);
    }

    EnvironmentConfiguration(String name, AbstractConfigurationValue<?> defaultValue, boolean disabled) {
        this(name, defaultValue, disabled, null);
    }

    EnvironmentConfiguration(String name, AbstractConfigurationValue<?> defaultValue, BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<EnvironmentConfiguration>> changeAction) {
        this(name, defaultValue, false, changeAction);
    }

    EnvironmentConfiguration(String name, AbstractConfigurationValue<?> defaultValue, boolean disabled, BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<EnvironmentConfiguration>> changeAction) {
        this.defaultValue = defaultValue;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) defaultValue.getClass();
        this.name = name;
        this.changeAction = changeAction;
        this.disabled = disabled;
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

    @Override
    public boolean isDisabled() {
        return this.disabled;
    }
}
