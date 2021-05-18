package de.emaeuer.optimization.paco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.pheromone.PacoPheromone;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PacoHandler extends OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(PacoHandler.class);

    private PacoPheromone pheromone;

    private final List<PacoAnt> currentAnts = new ArrayList<>();

    private final ConfigurationHandler<PacoConfiguration> configuration;
    private final StateHandler<PacoState> state = new StateHandler<>(PacoState.class);

    public PacoHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, PacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);

        // register own state in optimization state
        generalState.addNewValue(OptimizationState.IMPLEMENTATION_STATE, this.state);

        initialize();
    }

    private void initialize() {
        // build basic neural network with just the necessary network neurons and connections
        NeuralNetwork baseNetwork = NeuronBasedNeuralNetworkBuilder.buildWithConfiguration(ConfigurationHelper.extractEmbeddedConfiguration(getOptimizationConfiguration(), NeuralNetworkConfiguration.class, OptimizationConfiguration.NEURAL_NETWORK_CONFIGURATION))
                .implicitBias()
                .inputLayer()
                .fullyConnectToNextLayer()
                .outputLayer()
                .finish();

        this.pheromone = new PacoPheromone(this.configuration, baseNetwork);
    }

    @Override
    public void resetAndRestart() {
        super.resetAndRestart();

        this.state.resetValue(PacoState.CONNECTION_WEIGHTS_SCATTERED);

        initialize();
    }

    @Override
    protected List<? extends Solution> generateSolutions() {
        this.currentAnts.clear();

        // if pheromone matrix is empty create the necessary number of ants to fill the population completely
        int antsPerIteration = this.configuration.getValue(ANTS_PER_ITERATION, Integer.class);
        if (this.pheromone.getPopulationSize() < this.pheromone.getMaximalPopulationSize()) {
            antsPerIteration = Math.max(this.pheromone.getMaximalPopulationSize() - this.pheromone.getPopulationSize(), antsPerIteration);
        }

        IntStream.range(this.currentAnts.size(), antsPerIteration)
                .mapToObj(i -> this.pheromone.createAntFromPopulation())
                .forEach(this.currentAnts::add);

        return this.currentAnts;
    }

    @Override
    public void update() {
        PacoAnt bestOfThisIteration;
        if (this.pheromone.getPopulationSize() == 0) {
            // initially all ant update regardless of the fitness to fill the population
            bestOfThisIteration = this.currentAnts.stream()
                    .peek(this.pheromone::addAntToPopulation)
                    .max(Comparator.comparingDouble(PacoAnt::getFitness))
                    .orElse(null);
        } else if (this.pheromone.getPopulationSize() < this.pheromone.getMaximalPopulationSize() - this.configuration.getValue(UPDATES_PER_ITERATION, Integer.class)) {
            int skipCount = Math.max(0, this.currentAnts.size() - this.pheromone.getMaximalPopulationSize() + this.pheromone.getPopulationSize());
            bestOfThisIteration = this.currentAnts.stream()
                    .sorted(Comparator.comparingDouble(PacoAnt::getFitness))
                    .skip(skipCount)
                    .peek(this.pheromone::addAntToPopulation)
                    .max(Comparator.comparingDouble(PacoAnt::getFitness))
                    .orElse(null);
        } else {
            bestOfThisIteration = this.currentAnts.stream()
                    .sorted(Comparator.comparingDouble(PacoAnt::getFitness))
                    .skip(this.currentAnts.size() - this.configuration.getValue(UPDATES_PER_ITERATION, Integer.class))
                    .peek(this.pheromone::addAntToPopulation)
                    .max(Comparator.comparingDouble(PacoAnt::getFitness))
                    .orElse(null);
        }

        this.pheromone.exportPheromoneMatrixState(getEvaluationCounter(), this.state);

        if (bestOfThisIteration != null) {
            // copy best to prevent further modification because of references in pheromone matrix
            PacoAnt bestCopy = new PacoAnt(bestOfThisIteration.getNeuralNetwork().copy());
            bestCopy.setFitness(bestOfThisIteration.getFitness());
            setCurrentlyBestSolution(bestCopy);
        }

        super.update();
    }

    @Override
    protected void handleProgressionStagnation() {
        super.handleProgressionStagnation();
    }

    @Override
    protected List<? extends Solution> getCurrentSolutions() {
        return this.currentAnts;
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        return this.currentAnts
                .stream()
                .mapToDouble(PacoAnt::getFitness)
                .summaryStatistics();
    }
}
