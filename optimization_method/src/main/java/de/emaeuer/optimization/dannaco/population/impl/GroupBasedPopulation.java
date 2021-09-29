package de.emaeuer.optimization.dannaco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.Ant;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;
import java.util.stream.IntStream;

public class GroupBasedPopulation extends AgeBasedPopulation {

    private final Set<Integer> addedGroups = new HashSet<>();

    public GroupBasedPopulation(ConfigurationHandler<DannacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, baseNetwork, rng);
    }

    @Override
    public void updatePheromone() {
        addedGroups.clear();

        getCurrentAnts().sort(Comparator.comparingDouble(Ant::getGeneralizationCapability)
                .thenComparingDouble(Ant::getFitness)
                .reversed());
        int remainingAntUpdates = calculateNumberOfAntsToAdd();
        for (Ant ant : getCurrentAnts()) {
            if (remainingAntUpdates <= 0) {
                break;
            }

            Optional<Ant> addResult = addAnt(ant);

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
    public Optional<Ant> addAnt(Ant ant) {
        int group = ant.getTopologyData().getTopologyGroupID();

        if (getSize() < getMaxSize() || !this.addedGroups.contains(group)) {
            this.addedGroups.add(group);
            return super.addAnt(ant);
        }

        return Optional.empty();
    }
}
