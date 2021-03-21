package de.emaeuer.optimization.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.aco.configuration.AcoParameter;
import de.emaeuer.optimization.factory.OptimizationConfigFactory;

import java.util.function.BiConsumer;

public enum OptimizationConfiguration implements DefaultConfiguration<OptimizationConfiguration> {
    NN_INPUT_LAYER_SIZE("Neural network input layer size", new IntegerConfigurationValue(1, 1, Integer.MAX_VALUE)),
    NN_OUTPUT_LAYER_SIZE("Neural network output layer size", new IntegerConfigurationValue(1, 1, Integer.MAX_VALUE)),

    OPTIMIZATION_MAX_FITNESS_SCORE("Fitness threshold", new DoubleConfigurationValue(1000, 50, Double.MAX_VALUE)),
    OPTIMIZATION_MAX_NUMBER_OF_EVALUATIONS("Maximal number of evaluations", new IntegerConfigurationValue(20000, 10, Integer.MAX_VALUE)),
    OPTIMIZATION_PROGRESSION_THRESHOLD("Minimum fitness increase for progression", new DoubleConfigurationValue(0.2)),
    OPTIMIZATION_PROGRESSION_ITERATIONS("Threshold for number of iterations without progress", new IntegerConfigurationValue(5, 1, Integer.MAX_VALUE)),
    OPTIMIZATION_CONFIGURATION("The configuration of the selected optimization method", new EmbeddedConfiguration<>(OptimizationConfigFactory.createOptimizationConfiguration(OptimizationMethodNames.ACO))),
    OPTIMIZATION_METHOD_NAME("The name of the optimization method", new StringConfigurationValue("ACO", OptimizationMethodNames.getNames()),
            (v, h) -> {
                OptimizationMethodNames methodName = OptimizationMethodNames.valueOf(v.getStringRepresentation());
                ConfigurationHandler<?> configuration = OptimizationConfigFactory.createOptimizationConfiguration(methodName);
                h.setValue(OptimizationConfiguration.OPTIMIZATION_CONFIGURATION, new EmbeddedConfiguration<>(configuration));
            });

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;
    private final BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<OptimizationConfiguration>> changeAction;

    OptimizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this.defaultValue = defaultValue;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) defaultValue.getClass();
        this.name = name;
        this.changeAction = null;
    }

    OptimizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue, BiConsumer<AbstractConfigurationValue<?>, ConfigurationHandler<OptimizationConfiguration>> changeAction) {
        this.defaultValue = defaultValue;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) defaultValue.getClass();
        this.name = name;
        this.changeAction = changeAction;
    }

    OptimizationConfiguration(String name, Class<?> type) {
        this.defaultValue = null;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) type;
        this.name = name;
        this.changeAction = null;
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
}
