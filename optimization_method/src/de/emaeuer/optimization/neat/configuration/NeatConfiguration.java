package de.emaeuer.optimization.neat.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

public enum NeatConfiguration implements DefaultConfiguration<NeatConfiguration> {
    SURVIVAL_RATE("Percentage of the population which survives and reproduces", new DoubleConfigurationValue(0.2, 0.1, 0.5)),
    TOPOLOGY_MUTATION_CLASSIC("Topology mutation mode (true = original NEAT, false = ANJI", new BooleanConfigurationValue(false)),
    POPULATION_SIZE("Number of individuals in the starting population", new IntegerConfigurationValue(50, 50, 1000)),
    CHROM_COMPAT_EXCESS_COEFF("Adjustment of compatibility value based on the number of excess genes", new DoubleConfigurationValue(1)),
    CHROM_COMPAT_DISJOINT_COEFF("Adjustment of compatibility value based on the number of disjoint genes", new DoubleConfigurationValue(1)),
    CHROM_COMPAT_COMMON_COEFF("Adjustment of compatibility value based on the differences in the values of common connection weights", new DoubleConfigurationValue(0.4)),
    SPECIATION_THRESHOLD("compatibility threshold used to determine whether two individuals belong to the same species", new DoubleConfigurationValue(0.2, 0.1, 1.0)),
    ELITISM("Fittest individuals from species are copied unchanged into the next generation", new BooleanConfigurationValue(true)),
    ELITISM_MIN_SPECIE_SIZE("Minimum number of individuals a specie must contain for its fittest member to be copied unchanged into the next generation", new IntegerConfigurationValue(1, 1, 5)),
    WEIGHTED_SELECTOR("Determines whether or not roulette selection is used", new BooleanConfigurationValue(false)),
    // TODO following topology related configurations maybe should be in OptimizationConfiguration
    WEIGHT_MAX("Upper bound for connection weights", new DoubleConfigurationValue(1, 1, 500)),
    WEIGHT_MIN("Lower bound for connection weights", new DoubleConfigurationValue(-1, -500, -1)),
    INITIAL_TOPOLOGY_FULLY_CONNECTED("Begin with all input nodes being fully connected", new BooleanConfigurationValue(true)),
    INITIAL_TOPOLOGY_NUM_HIDDEN_NEURONS("Number of hidden nodes in the starting topology", new IntegerConfigurationValue(0, 0, Integer.MAX_VALUE)),
    INITIAL_TOPOLOGY_ACTIVATION("Activation function of hidden neurons", new StringConfigurationValue("sigmoid", "linear", "sigmoid", "evsail-sigmoid", "tanh", "tanh-cubic", "step", "signed", "clamped-linear", "signed-clamped-linear")),
    INITIAL_TOPOLOGY_ACTIVATION_INPUT("Activation function of input neurons", new StringConfigurationValue("sigmoid", "linear", "sigmoid", "evsail-sigmoid", "tanh", "tanh-cubic", "step", "signed", "clamped-linear", "signed-clamped-linear")),
    INITIAL_TOPOLOGY_ACTIVATION_OUTPUT("Activation function of hidden neurons", new StringConfigurationValue("sigmoid", "linear", "sigmoid", "evsail-sigmoid", "tanh", "tanh-cubic", "step", "signed", "clamped-linear", "signed-clamped-linear"));

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
    public void executeChangeAction(AbstractConfigurationValue<?> newValue, ConfigurationHandler<NeatConfiguration> handler) {
        // do nothing because not needed
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
