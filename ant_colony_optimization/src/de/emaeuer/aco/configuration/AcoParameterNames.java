package de.emaeuer.aco.configuration;

import de.emaeuer.optimization.configuration.OptimizationParameterNames;

public enum AcoParameterNames implements OptimizationParameterNames {

    PHEROMONE("p", "Pheromone value"),
    NUMBER_OF_DECISIONS("n", "Number of possible decisions");

    private final String name;
    private final String description;

    AcoParameterNames(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
