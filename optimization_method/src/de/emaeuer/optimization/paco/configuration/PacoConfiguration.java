package de.emaeuer.optimization.paco.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;
import de.emaeuer.optimization.paco.population.PopulationUpdateStrategies;

public enum PacoConfiguration implements DefaultConfiguration<PacoConfiguration> {

    POPULATION_SIZE("Solution population size", new IntegerConfigurationValue(20, 1, 100)),
    UPDATES_PER_ITERATION("Number ants that update each iteration", new IntegerConfigurationValue(2, 1, 10)),
    ANTS_PER_ITERATION("Number of ants per iteration", new IntegerConfigurationValue(10, 1, 100)),
    DEVIATION_FUNCTION("Function to calculate deviation", new ExpressionConfigurationValue("s/(p - 1) + 0.05", PacoParameter.class)),
    ELITISM("Use elitism", new BooleanConfigurationValue(false)),
    UPDATE_STRATEGY("Population update strategy", new StringConfigurationValue(PopulationUpdateStrategies.AGE.name(), PopulationUpdateStrategies.getNames())),
    DYNAMIC_PROBABILITY("Probability for dynamic change", new ExpressionConfigurationValue("0.75(n/p)^2+0.1", PacoParameter.class)),
    PHEROMONE_VALUE("Pheromone value of a connection", new ExpressionConfigurationValue("(n+1) / (p+1)", PacoParameter.class)),
    ENABLE_NEURON_ISOLATION("Enable neuron isolation", new BooleanConfigurationValue(false)),
    SPLIT_THRESHOLD("Threshold for splitting a connection instead of removing", new ExpressionConfigurationValue("leq(s/(p - 1), 0.1) * geq(n, p * 0.9)", PacoParameter.class));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;

    PacoConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this.defaultValue = defaultValue;
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
        return this.defaultValue.getClass();
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
