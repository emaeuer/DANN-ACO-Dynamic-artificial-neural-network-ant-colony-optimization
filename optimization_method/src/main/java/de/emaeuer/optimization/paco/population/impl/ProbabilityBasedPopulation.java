package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.AbstractPopulation;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;
import java.util.stream.IntStream;

public class ProbabilityBasedPopulation extends AbstractPopulation<List<PacoAnt>> {

    private final List<PacoAnt> removedAnts = new ArrayList<>();

    public ProbabilityBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, new ArrayList<>(), baseNetwork, rng);
    }

    public void updatePheromone() {
        getCurrentAnts().stream()
                .sorted(Comparator.comparingDouble(PacoAnt::getFitness).reversed())
                .limit(calculateNumberOfAntsToAdd())
                .map(this::addAnt)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(getPheromone()::addAnt);

        this.removedAnts.forEach(getPheromone()::removeAnt);
        this.removedAnts.clear();
    }

    @Override
    public Optional<PacoAnt> addAnt(PacoAnt ant) {
        checkAndSetIfGlobalBest(ant);
        getPopulation().add(ant);

        // because the new ant could already get removed calculate the next ant to get removed here
        // --> necessary to return nothing if the ant wasn't really added
        PacoAnt removedAnt = null;
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

    protected void addAntToRemove(PacoAnt ant) {
        if (ant != null) {
            this.removedAnts.add(ant);
        }
    }

    protected PacoAnt determineAntToRemove() {
        double[] removeProbabilities = calculateRemoveProbabilities();
        int indexToRemove = getRNG().selectRandomElementFromVector(removeProbabilities);
        PacoAnt antToRemove = getPopulation().get(indexToRemove);

        // if elitism is used select new ant to remove if the global best ant should be removed
        while (usesElitism() && antToRemove == getGlobalBest()) {
            indexToRemove = getRNG().selectRandomElementFromVector(removeProbabilities);
            antToRemove = getPopulation().get(indexToRemove);
        }

        return getPopulation().remove(indexToRemove);
    }

    private double[] calculateRemoveProbabilities() {
        DoubleSummaryStatistics fitnessSummary = getPopulation().stream()
                .mapToDouble(PacoAnt::getFitness)
                .summaryStatistics();

        double minimum = fitnessSummary.getMin();
        double average = (fitnessSummary.getSum() - minimum * getPopulation().size()) / getPopulation().size();

        return getPopulation().stream()
                .mapToDouble(PacoAnt::getFitness)
                .map(f -> f - minimum + average)
                .toArray();
    }

    @Override
    public Optional<PacoAnt> removeAnt() {
        // was already handled by the add method
        return Optional.empty();
    }
}
