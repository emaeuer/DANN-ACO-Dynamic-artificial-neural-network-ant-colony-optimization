package de.emaeuer.optimization.dannaco;

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
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.population.AbstractPopulation;
import de.emaeuer.optimization.dannaco.population.PopulationFactory;
import de.emaeuer.optimization.dannaco.state.DannacoRunState;
import de.emaeuer.optimization.dannaco.state.DannacoState;
import de.emaeuer.state.StateHandler;

import java.util.*;

public class DannacoHandler extends OptimizationMethod {

    private AbstractPopulation<?> population;

    private final ConfigurationHandler<DannacoConfiguration> configuration;
    private final StateHandler<DannacoRunState> runState;
    private final StateHandler<DannacoState> state;

    public DannacoHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, DannacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
        this.configuration.logConfiguration();

        //noinspection unchecked
        StateHandler<OptimizationRunState> runState = generalState.getValue(OptimizationState.STATE_OF_CURRENT_RUN, StateHandler.class);
        this.runState = new StateHandler<>(DannacoRunState.class, runState);
        this.runState.setName("DANN_ACO_RUN");

        this.state = new StateHandler<>(DannacoState.class, generalState);
        this.state.setName("DANNACO");

        // register own state in optimization state
        runState.execute(s -> s.addNewValue(OptimizationRunState.IMPLEMENTATION_RUN_STATE, this.runState));
        generalState.execute(s -> s.addNewValue(OptimizationState.IMPLEMENTATION_STATE, this.state));

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

        this.runState.execute(t -> t.resetValue(DannacoRunState.CONNECTION_WEIGHTS_SCATTERED));

        initialize();
    }

    @Override
    protected List<? extends Solution> generateSolutions() {
        return this.population.nextGeneration();
    }

    @Override
    public void update() {
        this.population.updatePheromone();

        Ant bestOfThisIteration = this.population.getCurrentAnts()
                .stream()
                .max(Comparator.comparingDouble(Ant::getGeneralizationCapability)
                        .thenComparingDouble(Ant::getFitness))
                .orElse(null);

        this.population.exportPheromoneMatrixState(getEvaluationCounter(), this.runState);
        this.population.exportCurrentGroups(getEvaluationCounter(), this.runState);
        this.population.exportModificationCounts(this.state);

        if (bestOfThisIteration != null) {
            // copy best to prevent further modification because of references in pheromone matrix
            Solution bestCopy = bestOfThisIteration.copy();
            bestCopy.setFitness(bestOfThisIteration.getFitness());
            bestCopy.setGeneralizationCapability(bestOfThisIteration.getGeneralizationCapability());
            setCurrentlyBestSolution(bestCopy);
        }

        super.update();
    }

    @Override
    protected void updateImplementationState() {
        this.population.exportDeviation(this.state);
        this.runState.execute(s -> {
            s.resetValue(DannacoRunState.CONNECTION_WEIGHTS_SCATTERED);
            s.resetValue(DannacoRunState.USED_GROUPS);
        });
    }

    @Override
    protected List<? extends Solution> getCurrentSolutions() {
        return this.population.getCurrentAnts();
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        return this.population.getCurrentAnts()
                .stream()
                .mapToDouble(Ant::getFitness)
                .summaryStatistics();
    }
}
