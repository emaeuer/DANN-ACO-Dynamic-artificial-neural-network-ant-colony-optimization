package de.emaeuer.optimization.dannaco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.Ant;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.population.AbstractPopulation;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;

public class FitnessBasedPopulation extends AbstractPopulation<PriorityQueue<Ant>> {

    public FitnessBasedPopulation(ConfigurationHandler<DannacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, new PriorityQueue<>(Comparator.comparingDouble(Ant::getGeneralizationCapability).thenComparingDouble(Ant::getFitness)), baseNetwork, rng);
    }

    @Override
    public Optional<Ant> addAnt(Ant ant) {
        checkAndSetIfGlobalBest(ant);

        double minFitnessOfPopulation = Optional.ofNullable(getPopulation().peek())
                .map(Ant::getFitness)
                .orElse(0.0);

        // add only if better than worst element or population not full
        if (ant == null || getPopulation().size() < getMaxSize()) {
            getPopulation().add(ant);
            return Optional.ofNullable(ant);
        } else if (minFitnessOfPopulation >= ant.getFitness()) {
            return Optional.empty();
        }

        getPopulation().add(ant);
        return Optional.of(ant);
    }

    @Override
    public Optional<Ant> removeAnt() {
        // remove the worst solution if the population contains too many ants
        if (getPopulation().size() > getMaxSize()) {
            return Optional.ofNullable(getPopulation().poll());
        }

        return Optional.empty();
    }
}
