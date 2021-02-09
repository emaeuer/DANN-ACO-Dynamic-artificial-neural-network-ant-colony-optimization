package de.emaeuer.aco;

import de.emaeuer.aco.colony.AcoColony;
import de.emaeuer.aco.configuration.AcoConfiguration;
import de.emaeuer.aco.configuration.AcoConfigurationKeys;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AcoHandler implements OptimizationMethod {

    private final List<AcoColony> colonies = new ArrayList<>();

    private int generationCounter = 0;

    private List<DoubleSummaryStatistics> fitnessOfThisIteration;

    private final AcoConfiguration configuration;

    public AcoHandler(AcoConfiguration configuration) {
        this.configuration = configuration;

        // build basic neural network with just the necessary network neurons and connections
        NeuralNetwork basicNetwork = NeuralNetwork.build()
            .inputLayer(configuration.getValueAsInt(AcoConfigurationKeys.NN_INPUT_LAYER_SIZE))
            .fullyConnectToNextLayer()
            .outputLayer(configuration.getValueAsInt(AcoConfigurationKeys.NN_OUTPUT_LAYER_SIZE))
            .finish();

        // create aco colonies
        IntStream.range(0, configuration.getValueAsInt(AcoConfigurationKeys.ACO_NUMBER_OF_COLONIES))
                .mapToObj(i -> new AcoColony(basicNetwork.copy(), configuration))
                .forEach(colonies::add);
    }

    @Override
    public List<Solution> generateSolutions() {
        this.generationCounter++;
        return this.colonies.stream()
                .map(AcoColony::nextIteration)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void update() {
        this.colonies.forEach(AcoColony::updateSolutions);

        this.fitnessOfThisIteration = this.colonies.stream()
                .map(AcoColony::getIterationStatistic)
                .collect(Collectors.toList());

        AcoColony worst = this.colonies.get(0);
        AcoColony best = this.colonies.get(0);

        for (AcoColony colony: this.colonies) {
            worst = colony.getCurrentFitness() < worst.getCurrentFitness() ? colony : worst;
            best = colony.getCurrentFitness() > best.getCurrentFitness() ? colony : best;
        }

        System.out.printf("Generation %d [ %.1f : %.1f ]%n", this.generationCounter, worst.getCurrentFitness(), best.getCurrentFitness());

        // best is a lot better than worst --> worst gets solution of best
        if (worst.getCurrentFitness() * 10 < best.getCurrentFitness()) {
            worst.takeSolutionOf(best);
        }
    }

    @Override
    public List<DoubleSummaryStatistics> getStatisticsOfIteration() {
        return this.fitnessOfThisIteration;
    }

    @Override
    public int getIterationCount() {
        return this.generationCounter;
    }
}
