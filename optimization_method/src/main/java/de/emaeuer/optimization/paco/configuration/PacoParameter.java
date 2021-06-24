package de.emaeuer.optimization.paco.configuration;

import de.emaeuer.configuration.ConfigurationVariable;

public enum PacoParameter implements ConfigurationVariable {
    SUM_OF_DIFFERENCES("Squared sum of differences from mean", "s"),
    POPULATION_SIZE("Size of the population", "p"),
    NUMBER_OF_VALUES("Number of values in the population for this specific value", "n"),
    CONNECTION_PHEROMONE("Pheromone value of current connection", "c"),
    TOPOLOGY_PHEROMONE("Pheromone value of current topology", "t");

    private final String name;
    private final String abbreviation;

    PacoParameter(String name, String abbreviation) {
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
