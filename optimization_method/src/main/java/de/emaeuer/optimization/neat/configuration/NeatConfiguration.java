package de.emaeuer.optimization.neat.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;

public enum NeatConfiguration implements DefaultConfiguration<NeatConfiguration> {
    SURVIVAL_RATE("Percentage of the population which survives and reproduces", new DoubleConfigurationValue(0.2, 0.1, 0.5)),
    TOPOLOGY_MUTATION_CLASSIC("Topology mutation mode (true = original NEAT, false = ANJI", new BooleanConfigurationValue(true)),
    POPULATION_SIZE("Number of individuals in the starting population", new IntegerConfigurationValue(50, 5, 1000)),
    CHROM_COMPAT_EXCESS_COEFF("Adjustment of compatibility value based on the number of excess genes", new DoubleConfigurationValue(1)),
    CHROM_COMPAT_DISJOINT_COEFF("Adjustment of compatibility value based on the number of disjoint genes", new DoubleConfigurationValue(1)),
    CHROM_COMPAT_COMMON_COEFF("Adjustment of compatibility value based on the differences in the values of common connection weights", new DoubleConfigurationValue(0.4)),
    SPECIATION_THRESHOLD("compatibility threshold used to determine whether two individuals belong to the same species", new DoubleConfigurationValue(0.2, 0.1, 1.0)),
    ELITISM("Fittest individuals from species are copied unchanged into the next generation", new BooleanConfigurationValue(true)),
    ELITISM_MIN_SPECIE_SIZE("Minimum number of individuals a specie must contain for its fittest member to be copied unchanged into the next generation", new IntegerConfigurationValue(1, 1, 10)),
    WEIGHTED_SELECTOR("Determines whether or not roulette selection is used", new BooleanConfigurationValue(false)),
    ADD_CONNECTION_MUTATION_RATE("The probability of new connections being added", new DoubleConfigurationValue(0.025, 0, 1)),
    ADD_NEURON_MUTATION_RATE("The probability of new nodes being added", new DoubleConfigurationValue(0.015, 0, 1)),
    REMOVE_CONNECTION_MUTATION_RATE("The rate at which existing connections are removed", new DoubleConfigurationValue(0.0, 0, 1)),
    REMOVE_CONNECTION_MAX_WEIGHT("The magnitude of the weights to be removed by the remove connection operator", new DoubleConfigurationValue(1, 0, 1)),
    REMOVE_CONNECTION_STRATEGY("Strategy for removing connections", new StringConfigurationValue("skewed", "skewed", "small", "all")),
    PRUNE_MUTATION_RATE("Probability of removing isolated neurons from the chromosome", new DoubleConfigurationValue(1, 0, 1)),
    WEIGHT_MUTATION_RATE("The probability of existing connection weights being mutated", new DoubleConfigurationValue(0.72, 0, 1)),
    WEIGHT_MUTATION_DEVIATION("The standard deviation for weight mutation values", new DoubleConfigurationValue(1.5, 0, 10)),
    ID_FILE("Path to file which persists chromosome ids", new StringConfigurationValue("temp/id.xml")),
    NEAT_ID_FILE("Path to file which persists innovation ids", new StringConfigurationValue("temp/neatid.xml"));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;

    NeatConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
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
