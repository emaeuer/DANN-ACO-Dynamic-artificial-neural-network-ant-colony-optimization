package de.emaeuer.optimization.paco.pheromone;

import com.google.common.collect.*;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

import java.util.*;

public class FitnessPopulationBasedPheromone extends AbstractPopulationBasedPheromone {

    public FitnessPopulationBasedPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        super(configuration, baseNetwork);
    }

    @Override
    protected Collection<PacoAnt> getEmptyPopulation() {
        return MinMaxPriorityQueue.orderedBy(Comparator.comparingDouble(PacoAnt::getFitness))
                .maximumSize(getMaximalPopulationSize())
                .create();
    }

    @Override
    public void addAntToPopulation(PacoAnt ant) {
        // ant only updates only if it is at least as good as the worst of this population and the population is completely populated
        if (isPopulationCompletelyPopulated() && !getPopulation().isEmpty() && getPopulationImpl().peekFirst().getFitness() > ant.getFitness()) {
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
