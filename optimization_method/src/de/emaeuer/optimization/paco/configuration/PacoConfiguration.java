package de.emaeuer.optimization.paco.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;

public enum PacoConfiguration implements DefaultConfiguration<PacoConfiguration> {

    PACO_POPULATION_SIZE("Solution population size", new IntegerConfigurationValue(20, 1, 40)),
    PACO_UPDATES_PER_ITERATION("Number ants that update each iteration", new IntegerConfigurationValue(2, 1, 10)),
    PACO_ANTS_PER_ITERATION("Number of ants per iteration", new IntegerConfigurationValue(10, 1, 40)),
    PACO_DEVIATION_FUNCTION("Function to calculate deviation", new ExpressionConfigurationValue("tanh((s/p+0.2) / 2)", PacoParameter.class)),
    PACO_ADDITIONAL_CONNECTION_PROBABILITY_FUNCTION("Function to calculate the probability for an additional connection", new ExpressionConfigurationValue("0.7(n / p) + 0.1", PacoParameter.class)),
    PACO_SPLIT_PROBABILITY_FUNCTION("Function to calculate split probability of a connection", new ExpressionConfigurationValue("1/s * v", PacoParameter.class)),
    PACO_KEEP_BEST("Keep best ant for next iteration", new BooleanConfigurationValue(false)),
    REMOVE_WORST("Remove the worst (true) / oldest (false) and", new BooleanConfigurationValue(true));

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
    public void executeChangeAction(AbstractConfigurationValue<?> newValue, ConfigurationHandler<PacoConfiguration> handler) {
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
