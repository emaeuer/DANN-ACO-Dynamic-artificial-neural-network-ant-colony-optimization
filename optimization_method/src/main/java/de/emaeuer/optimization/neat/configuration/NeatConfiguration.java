package de.emaeuer.optimization.neat.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.BooleanConfigurationValue;
import de.emaeuer.configuration.value.DoubleConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;

public enum NeatConfiguration implements DefaultConfiguration<NeatConfiguration> {
    SURVIVAL_RATE("Percentage of the population which survives and reproduces", new DoubleConfigurationValue(0.2, 0.1, 0.5)),
    TOPOLOGY_MUTATION_CLASSIC("Topology mutation mode (true = original NEAT, false = ANJI", new BooleanConfigurationValue(true)),
    POPULATION_SIZE("Number of individuals in the starting population", new IntegerConfigurationValue(50, 50, 1000)),
    CHROM_COMPAT_EXCESS_COEFF("Adjustment of compatibility value based on the number of excess genes", new DoubleConfigurationValue(1)),
    CHROM_COMPAT_DISJOINT_COEFF("Adjustment of compatibility value based on the number of disjoint genes", new DoubleConfigurationValue(1)),
    CHROM_COMPAT_COMMON_COEFF("Adjustment of compatibility value based on the differences in the values of common connection weights", new DoubleConfigurationValue(0.4)),
    SPECIATION_THRESHOLD("compatibility threshold used to determine whether two individuals belong to the same species", new DoubleConfigurationValue(0.2, 0.1, 1.0)),
    ELITISM("Fittest individuals from species are copied unchanged into the next generation", new BooleanConfigurationValue(true)),
    ELITISM_MIN_SPECIE_SIZE("Minimum number of individuals a specie must contain for its fittest member to be copied unchanged into the next generation", new IntegerConfigurationValue(1, 1, 5)),
    WEIGHTED_SELECTOR("Determines whether or not roulette selection is used", new BooleanConfigurationValue(false));

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
