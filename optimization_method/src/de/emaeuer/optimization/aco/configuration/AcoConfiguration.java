package de.emaeuer.optimization.aco.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.ExpressionConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;

public enum AcoConfiguration implements DefaultConfiguration<AcoConfiguration> {

    ACO_CONNECTION_SPLIT_PROBABILITY("Probability for splitting a connection", new ExpressionConfigurationValue("0.2", AcoParameter.class)),
    ACO_NUMBER_OF_COLONIES("Number of aco colonies", new IntegerConfigurationValue(4, 1, Integer.MAX_VALUE)),
    ACO_COLONY_SIZE("Number of ants per colony", new IntegerConfigurationValue(10, 1, Integer.MAX_VALUE)),
    ACO_INITIAL_PHEROMONE_VALUE("Initial pheromone value", new ExpressionConfigurationValue("0.1", AcoParameter.class)),
    ACO_STANDARD_DEVIATION_FUNCTION("Standard deviation function", new ExpressionConfigurationValue("(1 - 0.5p) / (10p + 1)", AcoParameter.class)),
    ACO_PHEROMONE_UPDATE_FUNCTION("Pheromone update function", new ExpressionConfigurationValue("tanh(3np)/tanh(3max(1,n))", AcoParameter.class)),
    ACO_PHEROMONE_DISSIPATION_FUNCTION("Pheromone dissipation function", new ExpressionConfigurationValue("0.9p", AcoParameter.class)),
    ACO_NEW_CONNECTION_PROBABILITY("Probability for choosing a new connection", new ExpressionConfigurationValue("0.05", AcoParameter.class)),
    ACO_NEW_LAYER_SELECTION_PROBABILITY("Inverted probability distribution for random layer index", new ExpressionConfigurationValue("ceil(min(rd-r,d))", AcoParameter.class));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;

    AcoConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
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
