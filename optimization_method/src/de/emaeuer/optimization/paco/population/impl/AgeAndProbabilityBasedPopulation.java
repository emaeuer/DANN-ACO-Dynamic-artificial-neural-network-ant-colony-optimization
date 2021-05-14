package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.optimization.paco.PacoAnt;

import java.util.Optional;

public class AgeAndProbabilityBasedPopulation extends ProbabilityBasedPopulation {

    public AgeAndProbabilityBasedPopulation(int maxSize, boolean useElitism) {
        super(maxSize, useElitism);
    }

    @Override
    public Optional<PacoAnt> addAnt(PacoAnt ant) {
        checkAndSetIfGlobalBest(ant);

        // the difference to ProbabilityBasedPopulation is that the new ant is always added
        if (getPopulation().size() >= getMaxSize()) {
            determineAntToRemove();
        }

        getPopulation().add(ant);
        return Optional.ofNullable(ant);
    }
}
