package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.AbstractPopulation;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;

public class AgeBasedPopulation extends AbstractPopulation<LinkedList<PacoAnt>> {

    public AgeBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, new LinkedList<>(), baseNetwork, rng);
    }

    public AgeBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, List<NeuralNetwork> baseNetwork, RandomUtil rng) {
        super(configuration, new LinkedList<>(), baseNetwork, rng);
    }

    @Override
    public Optional<PacoAnt> addAnt(PacoAnt ant) {
        checkAndSetIfGlobalBest(ant);

        getPopulation().add(ant);
        return Optional.ofNullable(ant);
    }

    @Override
    public Optional<PacoAnt> removeAnt() {
        if (getPopulation().size() <= getMaxSize()) {
            return Optional.empty();
        } else if (usesElitism()) {
            return removeWithRespectToElitism();
        } else {
            return Optional.ofNullable(getPopulation().poll());
        }
    }

    private Optional<PacoAnt> removeWithRespectToElitism() {
        PacoAnt antToRemove = getPopulation().poll();

        // keep the global best ant ant in the population and remove the next element
        if (antToRemove == getGlobalBest()) {
            antToRemove = getPopulation().poll();
            // add global best element as first element --> gets removed as soon as a better solution is found
            getPopulation().addFirst(getGlobalBest());
        }

        return Optional.ofNullable(antToRemove);
    }
}
