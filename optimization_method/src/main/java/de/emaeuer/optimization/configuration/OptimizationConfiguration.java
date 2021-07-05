package de.emaeuer.optimization.configuration;

import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.factory.OptimizationConfigFactory;

import java.util.function.BiConsumer;

public enum OptimizationConfiguration implements DefaultConfiguration<OptimizationConfiguration> {
    SEED("Seed for generating the environment", new IntegerConfigurationValue(9369319), true),
    NEURAL_NETWORK_CONFIGURATION("Neural network configuration", new EmbeddedConfiguration<>(new ConfigurationHandler<>(NeuralNetworkConfiguration.class, "NEURAL_NETWORK"))),
    MAX_NUMBER_OF_EVALUATIONS("Maximal number of evaluations", new IntegerConfigurationValue(20000, 10, Integer.MAX_VALUE)),
    PROGRESSION_THRESHOLD("Minimum fitness increase for progression", new DoubleConfigurationValue(0)),
    PROGRESSION_ITERATIONS("Threshold for number of iterations without progress", new IntegerConfigurationValue(200, 1, Integer.MAX_VALUE)),
    NUMBER_OF_RUNS("Number of runs", new IntegerConfigurationValue(10, 1, Integer.MAX_VALUE)),
    MAX_FITNESS_SCORE("Fitness threshold", new DoubleConfigurationValue(1000, 50, Double.MAX_VALUE), true),
    GENERALIZATION_MAX_FITNESS_SCORE("Fitness threshold for generalization", new DoubleConfigurationValue(10000, 0, Double.MAX_VALUE), true),
    TEST_GENERALIZATION("Test the generalization capability", new BooleanConfigurationValue(false), true),
    IMPLEMENTATION_CONFIGURATION("The configuration of the selected optimization method", new EmbeddedConfiguration<>(OptimizationConfigFactory.createOptimizationConfiguration(OptimizationMethodNames.PACO))),
    METHOD_NAME("The name of the optimization method", new StringConfigurationValue("PACO", OptimizationMethodNames.getNames()),
            (v, h) -> {
                OptimizationMethodNames methodName = OptimizationMethodNames.valueOf(v.getStringRepresentation());
                ConfigurationHandler<?> configuration = OptimizationConfigFactory.createOptimizationConfiguration(methodName);
                h.setValue(OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION, new EmbeddedConfiguration<>(configuration));
            });

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;
    private final BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<OptimizationConfiguration>> changeAction;
    private final boolean disabled;

    OptimizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this(name, defaultValue, false, null);
    }

    OptimizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue, BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<OptimizationConfiguration>> changeAction) {
        this(name, defaultValue, false, changeAction);
    }

    OptimizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue, boolean disabled) {
        this(name, defaultValue, disabled, null);
    }

    OptimizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue, boolean disabled, BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<OptimizationConfiguration>> changeAction) {
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
    public Class<? extends AbstractConfigurationValue<?>> getValueType() {
        return this.type;
    }

    @Override
    public void executeChangeAction(AbstractConfigurationValue<?> newValue, ConfigurationHandler<OptimizationConfiguration> handler) {
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
