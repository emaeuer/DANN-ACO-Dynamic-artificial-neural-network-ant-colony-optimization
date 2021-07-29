package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;
import java.util.stream.IntStream;

public class GroupBasedPopulation extends AgeBasedPopulation {

    public GroupBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        super(configuration, baseNetwork, rng);
    }

    public GroupBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration, List<NeuralNetwork> baseNetwork, RandomUtil rng) {
        super(configuration, baseNetwork, rng);
    }

    @Override
    public void updatePheromone() {
        if (getSize() < getMaxSize()) {
            // age based strategy until population completely filled
            super.updatePheromone();
        }

        Map<Integer, PacoAnt> bestOfEachGroup = new HashMap<>();
        for (PacoAnt ant : getCurrentAnts()) {
            int groupID = ant.getTopologyData().getTopologyGroupID();
            if (!bestOfEachGroup.containsKey(groupID) || ant.getFitness() > bestOfEachGroup.get(groupID).getFitness()) {
                bestOfEachGroup.put(groupID, ant);
            }
        }

        bestOfEachGroup.values()
                .stream()
                .sorted(Comparator.comparingDouble(PacoAnt::getGeneralizationCapability)
                        .thenComparingDouble(PacoAnt::getFitness)
                        .reversed())
                .limit(calculateNumberOfAntsToAdd())
                .map(this::addAnt)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(getPheromone()::addAnt);

        IntStream.range(0, Math.max(0, getSize() - getMaxSize()))
                .mapToObj(i -> this.removeAnt())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(getPheromone()::removeAnt);
    }
}
