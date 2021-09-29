package de.emaeuer.optimization.dannaco.population;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.population.impl.*;
import de.emaeuer.optimization.util.RandomUtil;

public class PopulationFactory {

    private PopulationFactory() {}

    public static AbstractPopulation<?> create(ConfigurationHandler<DannacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        String strategy = configuration.getValue(DannacoConfiguration.UPDATE_STRATEGY, String.class);

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
