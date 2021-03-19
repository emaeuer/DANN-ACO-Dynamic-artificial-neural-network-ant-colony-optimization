package de.emaeuer.optimization.aco.configuration;

import de.emaeuer.configuration.ConfigurationVariable;

public enum AcoParameter implements ConfigurationVariable {
    PHEROMONE("Pheromone value", "p"),
    NUMBER_OF_DECISIONS("Possible decision count", "n"),
    CURRENT_LAYER_INDEX("Current layer index", "l"),
    NEURAL_NETWORK_DEPTH("Depth of neural network", "d"),
    RANDOM_PARAMETER("A random number between 0 and 1", "r");

    private final String name;
    private final String abbreviation;

    AcoParameter(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getEquationAbbreviation() {
        return this.abbreviation;
    }
}
