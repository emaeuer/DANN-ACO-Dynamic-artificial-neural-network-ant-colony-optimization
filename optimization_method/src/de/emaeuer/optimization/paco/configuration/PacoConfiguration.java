package de.emaeuer.optimization.paco.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;

public enum PacoConfiguration implements DefaultConfiguration<PacoConfiguration> {

    POPULATION_SIZE("Solution population size", new IntegerConfigurationValue(20, 1, 100)),
    UPDATES_PER_ITERATION("Number ants that update each iteration", new IntegerConfigurationValue(2, 1, 10)),
    ANTS_PER_ITERATION("Number of ants per iteration", new IntegerConfigurationValue(10, 1, 100)),
    DEVIATION_FUNCTION("Function to calculate deviation", new ExpressionConfigurationValue("s/(p - 1) + 0.01", PacoParameter.class)),
    ADDITIONAL_CONNECTION_PROBABILITY_FUNCTION("Function to calculate the probability for an additional connection", new ExpressionConfigurationValue("0.9((n/p)^2)*(v^2)+0.1", PacoParameter.class)),
    REMOVE_CONNECTION_PROBABILITY_FUNCTION("Function to calculate the probability to remove a connection", new ExpressionConfigurationValue("-0.5((n/p)^2)*(v^2)+0.5", PacoParameter.class)),
    SPLIT_PROBABILITY_FUNCTION("Function to calculate split probability of a connection", new ExpressionConfigurationValue("1/s * v", PacoParameter.class)),
    KEEP_BEST("Keep best ant for next iteration", new BooleanConfigurationValue(false)),
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
