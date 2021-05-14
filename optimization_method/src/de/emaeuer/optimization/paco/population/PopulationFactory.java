package de.emaeuer.optimization.paco.population;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.impl.AgeAndProbabilityBasedPopulation;
import de.emaeuer.optimization.paco.population.impl.AgeBasedPopulation;
import de.emaeuer.optimization.paco.population.impl.FitnessBasedPopulation;
import de.emaeuer.optimization.paco.population.impl.ProbabilityBasedPopulation;

public class PopulationFactory {

    private PopulationFactory() {}

    public static AbstractPopulation<?> create(ConfigurationHandler<PacoConfiguration> configuration) {
        String strategy = configuration.getValue(PacoConfiguration.UPDATE_STRATEGY, String.class);
        int maxSize = configuration.getValue(PacoConfiguration.POPULATION_SIZE, Integer.class);
        boolean useElitism = configuration.getValue(PacoConfiguration.ELITISM, Boolean.class);

        return switch (PopulationUpdateStrategies.valueOf(strategy)) {
            case AGE -> new AgeBasedPopulation(maxSize, useElitism);
            case PROBABILITY -> new ProbabilityBasedPopulation(maxSize, useElitism);
            case FITNESS -> new FitnessBasedPopulation(maxSize);
            case AGE_PROBABILITY -> new AgeAndProbabilityBasedPopulation(maxSize, useElitism);
        };
    }

}
