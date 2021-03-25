package de.emaeuer.optimization.paco.pheromone;

import com.google.common.collect.MinMaxPriorityQueue;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

public class AgePopulationBasedPheromone extends AbstractPopulationBasedPheromone {

    private PacoAnt populationBest = null;

    public AgePopulationBasedPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        super(configuration, baseNetwork);
    }

    @Override
    protected Collection<PacoAnt> getEmptyPopulation() {
        return new LinkedList<>();
    }

    @Override
    public void addAntToPopulation(PacoAnt ant) {
        if (populationBest == null || populationBest.getFitness() < ant.getFitness()) {
            this.populationBest = ant;
        }

        super.addAntToPopulation(ant);
    }

    @Override
    protected PacoAnt removeAndGetAnt() {
        return getPopulationImpl().poll();
    }

    @Override
    protected PacoAnt getBestAntOfPopulation() {
        return this.populationBest;
    }

    private Queue<PacoAnt> getPopulationImpl() {
        return (Queue<PacoAnt>) this.getPopulation();
    }
}
