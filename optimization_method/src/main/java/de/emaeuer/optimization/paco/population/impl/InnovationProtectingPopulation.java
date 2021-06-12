package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InnovationProtectingPopulation extends AgeBasedPopulation {

    private List<Set<String>> addedTopologies = new ArrayList<>();

    public InnovationProtectingPopulation(ConfigurationHandler<PacoConfiguration> configuration) {
        super(configuration);
    }

    public List<PacoAnt>[] acceptAntsOfThisIteration(List<PacoAnt> ants) {
        addedTopologies.clear();

        //noinspection unchecked only way to return class with generic is unsafe cast
        List<PacoAnt>[] populationChange = (List<PacoAnt>[]) new List[2];
        populationChange[0] = new ArrayList<>();

        ants.sort(Comparator.comparingDouble(PacoAnt::getFitness).reversed());
        int remainingAntUpdates = calculateNumberOfAntsToAdd();
        for (PacoAnt ant : ants) {
            if (remainingAntUpdates <= 0) {
                break;
            }

            Optional<PacoAnt> addResult = addAnt(ant);

            if (addResult.isPresent()) {
                populationChange[0].add(ant);
                remainingAntUpdates--;
            }
        }

        populationChange[1] = IntStream.range(0, populationChange[0].size())
                .mapToObj(i -> this.removeAnt())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return populationChange;
    }

    @Override
    public Optional<PacoAnt> addAnt(PacoAnt ant) {
        Set<String> topology = new HashSet<>();
        NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork())
                .forEachRemaining(c -> topology.add(c.start() + "" + c.end()));

        if (getSize() < getMaxSize()) {
            addedTopologies.add(topology);
            return super.addAnt(ant);
        } else if (this.addedTopologies.contains(topology)) {
            return Optional.empty();
        } else if (addedTopologies.isEmpty()) {
            // first ant of iteration is always added regardless of fitness
            addedTopologies.add(topology);
            return super.addAnt(ant);
        } else if (topologyIsDifferentEnough(topology)) {
            // of each topology maximal one ant is added per iteration
                addedTopologies.add(topology);
                return super.addAnt(ant);
        }

        return Optional.empty();
    }

    private boolean topologyIsDifferentEnough(Set<String> topology) {
        for (Set<String> addedTopology : this.addedTopologies) {
            Set<String> union = new HashSet<>(addedTopology);
            union.addAll(topology);

            Set<String> intersection = new HashSet<>(addedTopology);
            intersection.retainAll(topology);

            double jaccardSimilarity = ((double) intersection.size()) / union.size();

            if (jaccardSimilarity <= 0.7) {
                return true;
            }
        }
        return false;
    }

}
