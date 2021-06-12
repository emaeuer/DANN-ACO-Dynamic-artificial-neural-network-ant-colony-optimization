package de.emaeuer.optimization.paco.population.impl;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;

import java.util.Optional;

public class AgeAndProbabilityBasedPopulation extends ProbabilityBasedPopulation {

    public AgeAndProbabilityBasedPopulation(ConfigurationHandler<PacoConfiguration> configuration) {
        super(configuration);
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
