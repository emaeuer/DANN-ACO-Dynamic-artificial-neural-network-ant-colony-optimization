package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GroupBasedPopulation extends AgeBasedPopulation {

    private final Set<Integer> addedGroups = new HashSet<>();

    public GroupBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, baseNetwork, rng);
    }

    @Override
    public void updatePheromone() {
        addedGroups.clear();

        getCurrentAnts().sort(Comparator.comparingDouble(PacoAnt::getGeneralizationCapability)
                .thenComparingDouble(PacoAnt::getFitness)
                .reversed());
        int remainingAntUpdates = calculateNumberOfAntsToAdd();
        for (PacoAnt ant : getCurrentAnts()) {
            if (remainingAntUpdates <= 0) {
                break;
            }

            Optional<PacoAnt> addResult = addAnt(ant);

            if (addResult.isPresent()) {
                getPheromone().addAnt(ant);
                remainingAntUpdates--;
            }
        }

        IntStream.range(0, Math.max(0, getSize() - getMaxSize()))
                .mapToObj(i -> this.removeAnt())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(getPheromone()::removeAnt);
    }

    @Override
    public Optional<PacoAnt> addAnt(PacoAnt ant) {
        int group = ant.getTopologyData().getTopologyGroupID();

        if (getSize() < getMaxSize() || !this.addedGroups.contains(group)) {
            this.addedGroups.add(group);
            return super.addAnt(ant);
        }

        return Optional.empty();
    }

}
