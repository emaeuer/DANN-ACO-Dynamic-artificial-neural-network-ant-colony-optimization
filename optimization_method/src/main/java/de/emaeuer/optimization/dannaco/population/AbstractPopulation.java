package de.emaeuer.optimization.dannaco.population;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.Ant;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.pheromone.Pheromone;
import de.emaeuer.optimization.dannaco.state.DannacoRunState;
import de.emaeuer.optimization.dannaco.state.DannacoState;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.state.StateHandler;

import java.util.*;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration.ANTS_PER_ITERATION;

public abstract class AbstractPopulation<T extends Collection<Ant>> {

    private final ConfigurationHandler<DannacoConfiguration> configuration;

    private final boolean useElitism;

    private final int maxSize;
    private final int updatesPerIteration;
    private final Pheromone pheromone;

    private Ant globalBest = null;

    private final T population;

    private final List<Ant> currentAnts = new ArrayList<>();

    private final RandomUtil rng;

    protected AbstractPopulation(ConfigurationHandler<DannacoConfiguration> configuration, T emptyPopulation, NeuralNetwork baseNetwork, RandomUtil rng) {
        this.configuration = configuration;
        this.maxSize = configuration.getValue(DannacoConfiguration.POPULATION_SIZE, Integer.class);
        this.useElitism = configuration.getValue(DannacoConfiguration.ELITISM, Boolean.class);
        this.updatesPerIteration = configuration.getValue(DannacoConfiguration.UPDATES_PER_ITERATION, Integer.class);
        this.population = emptyPopulation;
        this.rng = rng;

        this.pheromone = new Pheromone(this.configuration, baseNetwork, rng);
    }

    public List<Ant> nextGeneration()  {
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
                .sorted(Comparator.comparingDouble(Ant::getGeneralizationCapability)
                        .thenComparingDouble(Ant::getFitness)
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

    public abstract Optional<Ant> addAnt(Ant ant);

    public abstract Optional<Ant> removeAnt();

    protected boolean checkAndSetIfGlobalBest(Ant ant) {
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

    protected Ant getGlobalBest() {
        return this.globalBest;
    }

    protected T getPopulation() {
        return this.population;
    }

    public int getSize() {
        return getPopulation().size();
    }

    public List<Ant> getCurrentAnts() {
        return currentAnts;
    }

    protected RandomUtil getRNG() {
        return rng;
    }

    protected Pheromone getPheromone() {
        return pheromone;
    }

    public void exportPheromoneMatrixState(int evaluationCounter, StateHandler<DannacoRunState> state) {
        this.pheromone.exportPheromoneMatrixState(evaluationCounter, state);
    }

    public void exportCurrentGroups(int evaluationNumber, StateHandler<DannacoRunState> state) {
        this.pheromone.exportCurrentGroups(evaluationNumber, state);
    }

    public void exportModificationCounts(StateHandler<DannacoState> state) {
        this.pheromone.exportModificationCounts(state);
    }

    public void exportDeviation(StateHandler<DannacoState> state) {
        this.pheromone.exportDeviation(state);
    }
}
