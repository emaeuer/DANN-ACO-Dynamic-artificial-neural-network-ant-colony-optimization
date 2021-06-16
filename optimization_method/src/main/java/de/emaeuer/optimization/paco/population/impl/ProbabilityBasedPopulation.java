package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.AbstractPopulation;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

public class ProbabilityBasedPopulation extends AbstractPopulation<List<PacoAnt>> {

    private PacoAnt removedAnt = null;

    public ProbabilityBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, new ArrayList<>(), baseNetwork, rng);
    }

    @Override
    public Optional<PacoAnt> addAnt(PacoAnt ant) {
        checkAndSetIfGlobalBest(ant);
        getPopulation().add(ant);

        // because the new ant could already get removed calculate the next ant to get removed here
        // --> necessary to return nothing if the ant wasn't really added
        if (getPopulation().size() > getMaxSize()) {
            determineAntToRemove();
        }

        if (ant == this.removedAnt) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(ant);
        }
    }

    protected void determineAntToRemove() {
        double[] removeProbabilities = calculateRemoveProbabilities();
        int indexToRemove = getRNG().selectRandomElementFromVector(removeProbabilities);
        PacoAnt antToRemove = getPopulation().get(indexToRemove);

        // if elitism is used select new ant to remove if the global best ant should be removed
        while (usesElitism() && antToRemove == getGlobalBest()) {
            indexToRemove = getRNG().selectRandomElementFromVector(removeProbabilities);
            antToRemove = getPopulation().get(indexToRemove);
        }

        this.removedAnt = getPopulation().remove(indexToRemove);
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
        Optional<PacoAnt> result = Optional.ofNullable(this.removedAnt);
        this.removedAnt = null;
        return result;
    }
}
