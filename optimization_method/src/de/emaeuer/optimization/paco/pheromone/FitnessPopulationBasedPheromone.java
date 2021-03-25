package de.emaeuer.optimization.paco.pheromone;

import com.google.common.collect.*;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil.Connection;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariablesBuilder;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.configuration.PacoParameter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class FitnessPopulationBasedPheromone extends AbstractPopulationBasedPheromone {

    public FitnessPopulationBasedPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        super(configuration, baseNetwork);
    }

    @Override
    protected Collection<PacoAnt> getEmptyPopulation() {
        return MinMaxPriorityQueue.orderedBy(Comparator.comparingDouble(PacoAnt::getFitness))
                .maximumSize(getPopulationSize())
                .create();
    }

    @Override
    public void addAntToPopulation(PacoAnt ant) {
        // ant only updates only if it is at least as good as the worst of this population
        if (!getPopulation().isEmpty() && getPopulationImpl().peekFirst().getFitness() > ant.getFitness()) {
            return;
        }

        super.addAntToPopulation(ant);
    }

    @Override
    protected PacoAnt removeAndGetAnt() {
        return getPopulationImpl().pollFirst();
    }

    @Override
    protected PacoAnt getBestAntOfPopulation() {
        return getPopulationImpl().peekLast();
    }

    private MinMaxPriorityQueue<PacoAnt> getPopulationImpl() {
        return (MinMaxPriorityQueue<PacoAnt>) this.getPopulation();
    }
}
