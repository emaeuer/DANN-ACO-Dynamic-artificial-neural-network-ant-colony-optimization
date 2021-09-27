package de.emaeuer.optimization.paco.population;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.impl.*;
import de.emaeuer.optimization.util.RandomUtil;

public class PopulationFactory {

    private PopulationFactory() {}

    public static AbstractPopulation<?> create(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        String strategy = configuration.getValue(PacoConfiguration.UPDATE_STRATEGY, String.class);

        return switch (PopulationUpdateStrategies.valueOf(strategy)) {
            case AGE -> new AgeBasedPopulation(configuration, baseNetwork, rng);
            case PROBABILITY -> new ProbabilityBasedPopulation(configuration, baseNetwork, rng);
            case FITNESS -> new FitnessBasedPopulation(configuration, baseNetwork, rng);
            case AGE_PROBABILITY -> new AgeAndProbabilityBasedPopulation(configuration, baseNetwork, rng);
            case SIMILARITY -> new InnovationProtectingPopulation(configuration, baseNetwork, rng);
            case GROUP_BASED -> new GroupBasedPopulation(configuration, baseNetwork, rng);
        };
    }

}
