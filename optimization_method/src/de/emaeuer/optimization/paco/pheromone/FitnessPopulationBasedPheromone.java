package de.emaeuer.optimization.paco.pheromone;

import com.google.common.collect.*;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

import java.util.*;

public class FitnessPopulationBasedPheromone extends AbstractPopulationBasedPheromone {

    private final MinMaxPriorityQueue<PacoAnt> population;

    public FitnessPopulationBasedPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        super(configuration, baseNetwork);

        this.population = MinMaxPriorityQueue.orderedBy(Comparator.comparingDouble(PacoAnt::getFitness))
                .maximumSize(getMaximalPopulationSize())
                .create();
    }

    @Override
    public void addAntToPopulation(PacoAnt ant) {
        // ant only updates only if it is at least as good as the worst of this population and the population is completely populated
        if (isPopulationCompletelyPopulated() && !getPopulation().isEmpty() && this.population.peekFirst().getFitness() > ant.getFitness()) {
            return;
        }

        super.addAntToPopulation(ant);
    }

    @Override
    protected PacoAnt removeAndGetAnt() {
        return this.population.pollFirst();
    }

    @Override
    protected PacoAnt getBestAntOfPopulation() {
        return this.population.peekLast();
    }

    @Override
    public Collection<PacoAnt> getPopulation() {
        return this.population;
    }
}
