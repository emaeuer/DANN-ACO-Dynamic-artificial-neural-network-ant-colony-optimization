package de.emaeuer.evaluation;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;

import java.util.function.BiConsumer;

public enum EvaluationConfiguration implements DefaultConfiguration<EvaluationConfiguration> {
    MAX_TIME("Maximum optimization time in seconds", new IntegerConfigurationValue(0, 0, Integer.MAX_VALUE)),
    OPTIMIZATION_CONFIGURATION("Optimization configuration", new EmbeddedConfiguration<>(new ConfigurationHandler<>(OptimizationConfiguration.class))),
    ENVIRONMENT_CONFIGURATION("Environment configuration", new EmbeddedConfiguration<>(new ConfigurationHandler<>(EnvironmentConfiguration.class))),
    SEED("Seed of the evaluation", new IntegerConfigurationValue(9369319),
            (v, h) -> {
                ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(h, OptimizationConfiguration.class, OPTIMIZATION_CONFIGURATION);
                ConfigurationHandler<EnvironmentConfiguration> environmentConfig = ConfigurationHelper.extractEmbeddedConfiguration(h, EnvironmentConfiguration.class, ENVIRONMENT_CONFIGURATION);
                optimizationConfig.setValue(OptimizationConfiguration.SEED, v.getStringRepresentation());
                environmentConfig.setValue(EnvironmentConfiguration.SEED, v.getStringRepresentation());
        }),
    MAX_FITNESS_SCORE("Fitness threshold", new DoubleConfigurationValue(1, 0, Double.MAX_VALUE),
            (v, h) -> {
                ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(h, OptimizationConfiguration.class, OPTIMIZATION_CONFIGURATION);
                optimizationConfig.setValue(OptimizationConfiguration.MAX_FITNESS_SCORE, v.getStringRepresentation());
            }),
    GENERALIZATION_MAX_FITNESS_SCORE("Fitness threshold for generalization", new DoubleConfigurationValue(10000, 0, Double.MAX_VALUE),
            (v, h) -> {
                ConfigurationHandler<EnvironmentConfiguration> environmentConfig = ConfigurationHelper.extractEmbeddedConfiguration(h, EnvironmentConfiguration.class, ENVIRONMENT_CONFIGURATION);
                environmentConfig.setValue(EnvironmentConfiguration.GENERALIZATION_MAX_FITNESS_SCORE, v.getStringRepresentation());
            }),
    GENERALIZATION_CAPABILITY("Generalization capability threshold", new DoubleConfigurationValue(0.5, 0, 1),
            (v, h) -> {
                ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(h, OptimizationConfiguration.class, OPTIMIZATION_CONFIGURATION);
                optimizationConfig.setValue(OptimizationConfiguration.GENERALIZATION_CAPABILITY_THRESHOLD, v.getStringRepresentation());
            }),
    TEST_GENERALIZATION("Test the generalization capability", new BooleanConfigurationValue(true),
            (v, h) -> {
                ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(h, OptimizationConfiguration.class, OPTIMIZATION_CONFIGURATION);
                ConfigurationHandler<EnvironmentConfiguration> environmentConfig = ConfigurationHelper.extractEmbeddedConfiguration(h, EnvironmentConfiguration.class, ENVIRONMENT_CONFIGURATION);
                optimizationConfig.setValue(OptimizationConfiguration.TEST_GENERALIZATION, v.getStringRepresentation());
                environmentConfig.setValue(EnvironmentConfiguration.TEST_GENERALIZATION, v.getStringRepresentation());
                boolean value = Boolean.parseBoolean(v.getStringRepresentation());
                h.disableConfiguration(GENERALIZATION_MAX_FITNESS_SCORE, !value);
                h.disableConfiguration(GENERALIZATION_CAPABILITY, !value);
            });

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;
    private final BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<EvaluationConfiguration>> changeAction;

    EvaluationConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this(name, defaultValue, null);
    }

    EvaluationConfiguration(String name, AbstractConfigurationValue<?> defaultValue, BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<EvaluationConfiguration>> changeAction) {
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
    public void executeChangeAction(AbstractConfigurationValue<?> newValue, ConfigurationHandler<EvaluationConfiguration> handler) {
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
