package de.emaeuer.optimization.dannaco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.Ant;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.Optional;

public class AgeAndProbabilityBasedPopulation extends ProbabilityBasedPopulation {

    public AgeAndProbabilityBasedPopulation(ConfigurationHandler<DannacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, baseNetwork, rng);
    }

    @Override
    public Optional<Ant> addAnt(Ant ant) {
        checkAndSetIfGlobalBest(ant);

        // the difference to ProbabilityBasedPopulation is that the new ant is always added
        if (getPopulation().size() >= getMaxSize()) {
            addAntToRemove(determineAntToRemove());
        }

        getPopulation().add(ant);
        return Optional.ofNullable(ant);
    }
}
