package de.emaeuer.optimization.paco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationRunState;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.AbstractPopulation;
import de.emaeuer.optimization.paco.population.PopulationFactory;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.state.StateHandler;

import java.util.*;

public class PacoHandler extends OptimizationMethod {

    private AbstractPopulation<?> population;

    private final ConfigurationHandler<PacoConfiguration> configuration;
    private final StateHandler<PacoState> state;

    public PacoHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, PacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
        //noinspection unchecked
        StateHandler<OptimizationRunState> runState = generalState.getValue(OptimizationState.STATE_OF_CURRENT_RUN, StateHandler.class);
        this.state = new StateHandler<>(PacoState.class, runState);
        this.state.setName("PACO");

        this.configuration.logConfiguration();

        // register own state in optimization state
        runState.execute(s -> s.addNewValue(OptimizationRunState.IMPLEMENTATION_RUN_STATE, this.state));

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

        if (((getGenerationCounter() - 1) % (this.population.getMaxSize() / this.population.getUpdatesPerIteration())) == 0) {
            this.population.exportPheromoneMatrixState(getEvaluationCounter(), this.state);
        }
        this.population.exportCurrentGroups(getEvaluationCounter(), this.state);

        if (bestOfThisIteration != null) {
            // copy best to prevent further modification because of references in pheromone matrix
            PacoAnt bestCopy = new PacoAnt(bestOfThisIteration.getTopologyData().copy());
            bestCopy.setFitness(bestOfThisIteration.getFitness());
            setCurrentlyBestSolution(bestCopy);
        }

        super.update();
    }

    @Override
    protected void updateImplementationState() {
        this.state.execute(s -> {
            s.resetValue(PacoState.CONNECTION_WEIGHTS_SCATTERED);
            s.resetValue(PacoState.USED_GROUPS);
        });
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
