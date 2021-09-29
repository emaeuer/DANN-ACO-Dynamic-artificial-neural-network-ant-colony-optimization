package de.emaeuer.optimization.dannaco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.Ant;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.population.AbstractPopulation;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;

public class ProbabilityBasedPopulation extends AbstractPopulation<List<Ant>> {

    private final List<Ant> removedAnts = new ArrayList<>();

    public ProbabilityBasedPopulation(ConfigurationHandler<DannacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, new ArrayList<>(), baseNetwork, rng);
    }

    public void updatePheromone() {
        getCurrentAnts().stream()
                .sorted(Comparator.comparingDouble(Ant::getGeneralizationCapability)
                        .thenComparingDouble(Ant::getFitness)
                        .reversed())
                .limit(calculateNumberOfAntsToAdd())
                .map(this::addAnt)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(getPheromone()::addAnt);

        this.removedAnts.forEach(getPheromone()::removeAnt);
        this.removedAnts.clear();
    }

    @Override
    public Optional<Ant> addAnt(Ant ant) {
        checkAndSetIfGlobalBest(ant);
        getPopulation().add(ant);

        // because the new ant could already get removed calculate the next ant to get removed here
        // --> necessary to return nothing if the ant wasn't really added
        Ant removedAnt = null;
        if (getPopulation().size() > getMaxSize()) {
            removedAnt = determineAntToRemove();
        }

        if (ant == removedAnt) {
            return Optional.empty();
        } else {
            addAntToRemove(removedAnt);
            return Optional.ofNullable(ant);
        }
    }

    protected void addAntToRemove(Ant ant) {
        if (ant != null) {
            this.removedAnts.add(ant);
        }
    }

    protected Ant determineAntToRemove() {
        double[] removeProbabilities = calculateRemoveProbabilities();
        int indexToRemove = getRNG().selectRandomElementFromVector(removeProbabilities, true);
        Ant antToRemove = getPopulation().get(indexToRemove);

        // if elitism is used select new ant to remove if the global best ant should be removed
        while (usesElitism() && antToRemove == getGlobalBest()) {
            indexToRemove = getRNG().selectRandomElementFromVector(removeProbabilities);
            antToRemove = getPopulation().get(indexToRemove);
        }

        return getPopulation().remove(indexToRemove);
    }

    private double[] calculateRemoveProbabilities() {
        return getPopulation().stream()
                .mapToDouble(Ant::getFitness)
                .toArray();
    }

    @Override
    public Optional<Ant> removeAnt() {
        // was already handled by the add method
        return Optional.empty();
    }
}
