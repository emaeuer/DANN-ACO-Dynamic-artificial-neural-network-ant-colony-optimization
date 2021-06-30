package de.emaeuer.optimization.paco.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;
import de.emaeuer.optimization.paco.population.PopulationUpdateStrategies;

public enum PacoConfiguration implements DefaultConfiguration<PacoConfiguration> {

    POPULATION_SIZE("Solution population size", new IntegerConfigurationValue(20, 1, 100)),
    UPDATES_PER_ITERATION("Number ants that update each iteration", new IntegerConfigurationValue(1, 1, 10)),
    ANTS_PER_ITERATION("Number of ants per iteration", new IntegerConfigurationValue(10, 1, 100)),
    TOPOLOGY_PHEROMONE("Pheromone value of a topology", new ExpressionConfigurationValue("0.75(n/k)^2+0.1", PacoParameter.class)),
    CONNECTION_PHEROMONE("Pheromone value of a connection", new ExpressionConfigurationValue("0.75(n/k)+0.1", PacoParameter.class)),
    NUMBER_OF_VALUES_PENALTY("Penalty for too few values", new ExpressionConfigurationValue("(k-n)/(n+1)", PacoParameter.class)),
    DEVIATION_FUNCTION("Function to calculate deviation", new ExpressionConfigurationValue("(s+z)/(k - 1)", PacoParameter.class)),
    SPLIT_PROBABILITY("Probability for splitting a connection instead of removing", new ExpressionConfigurationValue("max(min((c*t)/d, 1), 0)", PacoParameter.class)),
    SOLUTION_WEIGHT_FACTOR("Deviation factor for calculation of the solution weights", new DoubleConfigurationValue(0.1, 0.000001, 1)),
    UPDATE_STRATEGY("Population update strategy", new StringConfigurationValue(PopulationUpdateStrategies.AGE.name(), PopulationUpdateStrategies.getNames())),
    ELITISM("Use elitism", new BooleanConfigurationValue(false)),
    ENABLE_NEURON_ISOLATION("Enable neuron isolation", new BooleanConfigurationValue(false)),
    REUSE_SPLIT_KNOWLEDGE("Link the first connection of a split to the knowledge of the old connection", new BooleanConfigurationValue(false));


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
