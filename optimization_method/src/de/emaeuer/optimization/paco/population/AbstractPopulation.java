package de.emaeuer.optimization.paco.population;

import de.emaeuer.optimization.paco.PacoAnt;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractPopulation<T extends Collection<PacoAnt>> {

    private final boolean useElitism;

    private final int maxSize;

    private PacoAnt globalBest = null;

    private final T population;

    protected AbstractPopulation(int maxSize, boolean useElitism, T emptyPopulation) {
        this.maxSize = maxSize;
        this.useElitism = useElitism;
        this.population = emptyPopulation;
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

    protected boolean usesElitism() {
        return this.useElitism;
    }

    protected PacoAnt getGlobalBest() {
        return this.globalBest;
    }

    protected void setGlobalBest(PacoAnt globalBest) {
        this.globalBest = globalBest;
    }

    protected T getPopulation() {
        return this.population;
    }

    public int getSize() {
        return getPopulation().size();
    }
}
