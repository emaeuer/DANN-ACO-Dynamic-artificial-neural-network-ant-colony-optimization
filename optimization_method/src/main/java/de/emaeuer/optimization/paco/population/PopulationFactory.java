package de.emaeuer.optimization.paco.population;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.impl.*;

public class PopulationFactory {

    private PopulationFactory() {}

    public static AbstractPopulation<?> create(ConfigurationHandler<PacoConfiguration> configuration) {
        String strategy = configuration.getValue(PacoConfiguration.UPDATE_STRATEGY, String.class);

        return switch (PopulationUpdateStrategies.valueOf(strategy)) {
            case AGE -> new AgeBasedPopulation(configuration);
            case PROBABILITY -> new ProbabilityBasedPopulation(configuration);
            case FITNESS -> new FitnessBasedPopulation(configuration);
            case AGE_PROBABILITY -> new AgeAndProbabilityBasedPopulation(configuration);
            case INNOVATION_PROTECTING -> new InnovationProtectingPopulation(configuration);
        };
    }

}
