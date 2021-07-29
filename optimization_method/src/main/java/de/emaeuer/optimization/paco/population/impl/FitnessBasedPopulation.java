package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.AbstractPopulation;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;

public class FitnessBasedPopulation extends AbstractPopulation<PriorityQueue<PacoAnt>> {

    public FitnessBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, new PriorityQueue<>(Comparator.comparingDouble(PacoAnt::getGeneralizationCapability).thenComparingDouble(PacoAnt::getFitness)), baseNetwork, rng);
    }

    public FitnessBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, List<NeuralNetwork> baseNetwork, RandomUtil rng) {
        super(configuration, new PriorityQueue<>(Comparator.comparingDouble(PacoAnt::getGeneralizationCapability).thenComparingDouble(PacoAnt::getFitness)), baseNetwork, rng);
    }

    @Override
    public Optional<PacoAnt> addAnt(PacoAnt ant) {
        checkAndSetIfGlobalBest(ant);

        double minFitnessOfPopulation = Optional.ofNullable(getPopulation().peek())
                .map(PacoAnt::getFitness)
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
    public Optional<PacoAnt> removeAnt() {
        // remove the worst solution if the population contains too many ants
        if (getPopulation().size() > getMaxSize()) {
            return Optional.ofNullable(getPopulation().poll());
        }

        return Optional.empty();
    }
}
