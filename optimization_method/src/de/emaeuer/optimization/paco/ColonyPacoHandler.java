package de.emaeuer.optimization.paco;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

public class ColonyPacoHandler extends OptimizationMethod {

    private final List<PacoHandler> colonies = new ArrayList<>();


    public ColonyPacoHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        for (int i = 0; i < 3; i++) {
            PacoHandler colony = new PacoHandler(configuration, new StateHandler<>(OptimizationState.class));

            for (int j = 0; j < i; j++) {
                colony.handleProgressionStagnation();
            }

            colonies.add(colony);
        }
    }

    @Override
    protected List<? extends Solution> generateSolutions() {
        return this.colonies.stream()
                .map(PacoHandler::generateSolutions)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void update() {
        this.colonies.forEach(PacoHandler::update);

        PacoAnt best = this.colonies
                .stream()
                .map(PacoHandler::getCurrentlyBestSolution)
                .map(PacoAnt.class::cast)
                .max(Comparator.comparingDouble(PacoAnt::getFitness))
                .orElse(null);

        setCurrentlyBestSolution(best);

        super.update();
    }

    @Override
    protected void handleProgressionStagnation() {
        super.handleProgressionStagnation();
        this.colonies.forEach(PacoHandler::handleProgressionStagnation);
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        this.colonies.stream()
                .map(PacoHandler::getFitnessOfIteration)
                .forEach(statistics::combine);

        return statistics;
    }
}
