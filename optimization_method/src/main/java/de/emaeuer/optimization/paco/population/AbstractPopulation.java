package de.emaeuer.optimization.paco.population;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractPopulation<T extends Collection<PacoAnt>> {

    private final ConfigurationHandler<PacoConfiguration> configuration;

    private final boolean useElitism;

    private final int maxSize;
    private final int updatesPerIteration;

    private PacoAnt globalBest = null;

    private final T population;

    protected AbstractPopulation(ConfigurationHandler<PacoConfiguration> configuration, T emptyPopulation) {
        this.configuration = configuration;
        this.maxSize = configuration.getValue(PacoConfiguration.POPULATION_SIZE, Integer.class);
        this.useElitism = configuration.getValue(PacoConfiguration.ELITISM, Boolean.class);
        this.updatesPerIteration = configuration.getValue(PacoConfiguration.UPDATES_PER_ITERATION, Integer.class);
        this.population = emptyPopulation;
    }

    public List<PacoAnt>[] acceptAntsOfThisIteration(List<PacoAnt> ants) {
        //noinspection unchecked only way to return class with generic is unsafe cast
        List<PacoAnt>[] populationChange = (List<PacoAnt>[]) new List[2];

        populationChange[0] = ants.stream()
                .sorted(Comparator.comparingDouble(PacoAnt::getFitness).reversed())
                .limit(calculateNumberOfAntsToAdd())
                .map(this::addAnt)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        populationChange[1] = IntStream.range(0, populationChange[0].size())
                .mapToObj(i -> this.removeAnt())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return populationChange;
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

    protected int getMaxSize() {
        return this.maxSize;
    }

    protected int getUpdatesPerIteration() {
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

}
