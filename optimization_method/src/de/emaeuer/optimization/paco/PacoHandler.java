package de.emaeuer.optimization.paco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.pheromone.PopulationBasedPheromone;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PacoHandler extends OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(PacoHandler.class);

    private final PopulationBasedPheromone pheromone;

    private final List<PacoAnt> currentAnts = new ArrayList<>();

    private final ConfigurationHandler<PacoConfiguration> configuration;

    public PacoHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, PacoConfiguration.class, OptimizationConfiguration.OPTIMIZATION_CONFIGURATION);

        // build basic neural network with just the necessary network neurons and connections
        NeuralNetwork baseNetwork = NeuralNetwork.build()
                .inputLayer(configuration.getValue(OptimizationConfiguration.NN_INPUT_LAYER_SIZE, Integer.class))
                .fullyConnectToNextLayer()
                .outputLayer(configuration.getValue(OptimizationConfiguration.NN_OUTPUT_LAYER_SIZE, Integer.class))
                .finish();

        this.pheromone = new PopulationBasedPheromone(this.configuration, baseNetwork);
    }

    @Override
    protected List<? extends Solution> generateSolutions() {
        this.currentAnts.clear();

        // if pheromone matrix is empty create the necessary number of ants to fill the population completely
        int antsPerIteration = this.pheromone.getPopulation().isEmpty()
                ? this.configuration.getValue(PACO_POPULATION_SIZE, Integer.class)
                : this.configuration.getValue(PACO_ANTS_PER_ITERATION, Integer.class);

        IntStream.range(0, antsPerIteration)
                .mapToObj(i -> this.pheromone.createNeuralNetworkForPheromone())
                .map(PacoAnt::new)
                .forEach(this.currentAnts::add);

        return this.currentAnts;
    }

    @Override
    public void update() {
        PacoAnt bestOfThisIteration;
        if (this.pheromone.getPopulation().isEmpty()) {
            // initially all ant update regardless of the fitness to fill the population
            bestOfThisIteration = this.currentAnts.stream()
                    .peek(this.pheromone::addAntToPopulation)
                    .max(Comparator.comparingDouble(PacoAnt::getFitness))
                    .orElse(null);
        } else {
            bestOfThisIteration = this.currentAnts.stream()
                    .sorted(Comparator.comparingDouble(PacoAnt::getFitness))
                    .skip(this.currentAnts.size() - this.configuration.getValue(PACO_UPDATES_PER_ITERATION, Integer.class))
                    .peek(this.pheromone::addAntToPopulation)
                    .max(Comparator.comparingDouble(PacoAnt::getFitness))
                    .orElse(null);
        }

        if (bestOfThisIteration != null) {
            // copy best to prevent further modification because of references in pheromone matrix
            PacoAnt bestCopy = new PacoAnt(bestOfThisIteration.getNeuralNetwork().copy());
            setCurrentlyBestSolution(bestOfThisIteration);
        }

        super.update();
    }

    @Override
    protected void handleProgressionStagnation() {
        super.handleProgressionStagnation();
        this.pheromone.increaseComplexity();
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        return this.currentAnts
                .stream()
                .mapToDouble(PacoAnt::getFitness)
                .summaryStatistics();
    }
}
