package de.emaeuer.optimization.paco.population;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.pheromone.PacoPheromone;
import de.emaeuer.optimization.paco.state.PacoRunState;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.state.StateHandler;

import java.util.*;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.ANTS_PER_ITERATION;

public abstract class AbstractPopulation<T extends Collection<PacoAnt>> {

    private final ConfigurationHandler<PacoConfiguration> configuration;

    private final boolean useElitism;

    private final int maxSize;
    private final int updatesPerIteration;
    private final PacoPheromone pheromone;

    private PacoAnt globalBest = null;

    private final T population;

    private final List<PacoAnt> currentAnts = new ArrayList<>();

    private final RandomUtil rng;

    protected AbstractPopulation(ConfigurationHandler<PacoConfiguration> configuration, T emptyPopulation, NeuralNetwork baseNetwork, RandomUtil rng) {
        this.configuration = configuration;
        this.maxSize = configuration.getValue(PacoConfiguration.POPULATION_SIZE, Integer.class);
        this.useElitism = configuration.getValue(PacoConfiguration.ELITISM, Boolean.class);
        this.updatesPerIteration = configuration.getValue(PacoConfiguration.UPDATES_PER_ITERATION, Integer.class);
        this.population = emptyPopulation;
        this.rng = rng;

        this.pheromone = new PacoPheromone(this.configuration, baseNetwork, rng);
    }

    public List<PacoAnt> nextGeneration()  {
        this.currentAnts.clear();

        // if pheromone matrix is empty create the necessary number of ants to fill the population completely
        int antsPerIteration = this.configuration.getValue(ANTS_PER_ITERATION, Integer.class);
        if (getSize() < getMaxSize()) {
            antsPerIteration = Math.max(getMaxSize() - getSize(), antsPerIteration);
        }

        IntStream.range(this.currentAnts.size(), antsPerIteration)
                .mapToObj(i -> this.pheromone.createAntFromPopulation())
                .forEach(this.currentAnts::add);

        return this.currentAnts;
    }

    public void updatePheromone() {
        this.currentAnts.stream()
                .sorted(Comparator.comparingDouble(PacoAnt::getGeneralizationCapability)
                        .thenComparingDouble(PacoAnt::getFitness)
                        .reversed())
                .limit(calculateNumberOfAntsToAdd())
                .map(this::addAnt)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(this.pheromone::addAnt);

        IntStream.range(0, Math.max(0, getSize() - getMaxSize()))
                .mapToObj(i -> this.removeAnt())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(this.pheromone::removeAnt);
    }

    protected int calculateNumberOfAntsToAdd() {
        if (getSize() < getMaxSize()) {
            return getMaxSize() - getSize();
        } else {
            return getUpdatesPerIteration();
        }
    }

    public abstract Optional<PacoAnt> addAnt(PacoAnt ant);

    public abstract Optional<PacoAnt> removeAnt();

    protected boolean checkAndSetIfGlobalBest(PacoAnt ant) {
        double globalBestFitness = this.globalBest == null ? 0 : this.globalBest.getFitness();
        if (ant != null && ant.getFitness() > globalBestFitness) {
            this.globalBest = ant;
            return true;
        }
        return false;
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public int getUpdatesPerIteration() {
        return updatesPerIteration;
    }

    protected boolean usesElitism() {
        return this.useElitism;
    }

    protected PacoAnt getGlobalBest() {
        return this.globalBest;
    }

    protected T getPopulation() {
        return this.population;
    }

    public int getSize() {
        return getPopulation().size();
    }

    public List<PacoAnt> getCurrentAnts() {
        return currentAnts;
    }

    protected RandomUtil getRNG() {
        return rng;
    }

    protected PacoPheromone getPheromone() {
        return pheromone;
    }

    public void exportPheromoneMatrixState(int evaluationCounter, StateHandler<PacoRunState> state) {
        this.pheromone.exportPheromoneMatrixState(evaluationCounter, state);
    }

    public void exportCurrentGroups(int evaluationNumber, StateHandler<PacoRunState> state) {
        this.pheromone.exportCurrentGroups(evaluationNumber, state);
    }

    public void exportModificationCounts(StateHandler<PacoState> state) {
        this.pheromone.exportModificationCounts(state);
    }

    public void exportDeviation(StateHandler<PacoState> state) {
        this.pheromone.exportDeviation(state);
    }
}
