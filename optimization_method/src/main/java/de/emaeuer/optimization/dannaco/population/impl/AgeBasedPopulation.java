package de.emaeuer.optimization.dannaco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.Ant;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.population.AbstractPopulation;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;

public class AgeBasedPopulation extends AbstractPopulation<LinkedList<Ant>> {

    public AgeBasedPopulation(ConfigurationHandler<DannacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, new LinkedList<>(), baseNetwork, rng);
    }

    @Override
    public Optional<Ant> addAnt(Ant ant) {
        checkAndSetIfGlobalBest(ant);

        getPopulation().add(ant);
        return Optional.ofNullable(ant);
    }

    @Override
    public Optional<Ant> removeAnt() {
        if (getPopulation().size() <= getMaxSize()) {
            return Optional.empty();
        } else if (usesElitism()) {
            return removeWithRespectToElitism();
        } else {
            return Optional.ofNullable(getPopulation().poll());
        }
    }

    private Optional<Ant> removeWithRespectToElitism() {
        Ant antToRemove = getPopulation().poll();

        // keep the global best ant ant in the population and remove the next element
        if (antToRemove == getGlobalBest()) {
            antToRemove = getPopulation().poll();
            // add global best element as first element --> gets removed as soon as a better solution is found
            getPopulation().addFirst(getGlobalBest());
        }

        return Optional.ofNullable(antToRemove);
    }
}
