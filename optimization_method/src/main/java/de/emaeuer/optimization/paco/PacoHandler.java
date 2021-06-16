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
import de.emaeuer.optimization.paco.population.AbstractPopulation;
import de.emaeuer.optimization.paco.population.PopulationFactory;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PacoHandler extends OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(PacoHandler.class);

    private AbstractPopulation<?> population;

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

        this.population = PopulationFactory.create(configuration, baseNetwork, getRNG());
    }

    @Override
    public void resetAndRestart() {
        super.resetAndRestart();

        this.state.execute(t -> t.resetValue(PacoState.CONNECTION_WEIGHTS_SCATTERED));

        initialize();
    }

    @Override
    protected List<? extends Solution> generateSolutions() {
        return this.population.nextGeneration();
    }

    @Override
    public void update() {
        this.population.updatePheromone();

        PacoAnt bestOfThisIteration = this.population.getCurrentAnts()
                .stream()
                .max(Comparator.comparingDouble(PacoAnt::getFitness))
                .orElse(null);

        // TODO implement again
        // this.pheromone.exportPheromoneMatrixState(getEvaluationCounter(), this.state);

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
        return this.population.getCurrentAnts();
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        return this.population.getCurrentAnts()
                .stream()
                .mapToDouble(PacoAnt::getFitness)
                .summaryStatistics();
    }
}
