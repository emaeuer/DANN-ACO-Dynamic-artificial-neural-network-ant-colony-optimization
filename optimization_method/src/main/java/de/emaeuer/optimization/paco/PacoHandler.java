package de.emaeuer.optimization.paco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.ann.util.NeuralNetworkUtil;
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
import de.emaeuer.optimization.paco.state.PacoRunState;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.state.StateHandler;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PacoHandler extends OptimizationMethod {

    private AbstractPopulation<?> population;

    private final ConfigurationHandler<PacoConfiguration> configuration;
    private final StateHandler<PacoRunState> runState;
    private final StateHandler<PacoState> state;

    public PacoHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, PacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
        this.configuration.logConfiguration();

        //noinspection unchecked
        StateHandler<OptimizationRunState> runState = generalState.getValue(OptimizationState.STATE_OF_CURRENT_RUN, StateHandler.class);
        this.runState = new StateHandler<>(PacoRunState.class, runState);
        this.runState.setName("PACO_RUN");

        this.state = new StateHandler<>(PacoState.class, generalState);
        this.state.setName("PACO");

        // register own state in optimization state
        runState.execute(s -> s.addNewValue(OptimizationRunState.IMPLEMENTATION_RUN_STATE, this.runState));
        generalState.execute(s -> s.addNewValue(OptimizationState.IMPLEMENTATION_STATE, this.state));

        initialize();
    }

    private void initialize() {
        // set true for ablation study
        if (false) {
            List<NeuralNetwork> baseNetworks = IntStream.range(0, this.configuration.getValue(PacoConfiguration.ANTS_PER_ITERATION, Integer.class))
                    .mapToObj(i -> this.createRandomBaseNetwork())
                    .toList();

            this.population = PopulationFactory.create(configuration, baseNetworks, getRNG());
            return;
        }

        // build basic neural network with just the necessary network neurons and connections
        NeuralNetwork baseNetwork = NeuronBasedNeuralNetworkBuilder.buildWithConfiguration(ConfigurationHelper.extractEmbeddedConfiguration(getOptimizationConfiguration(), NeuralNetworkConfiguration.class, OptimizationConfiguration.NEURAL_NETWORK_CONFIGURATION))
                .implicitBias()
                .inputLayer()
                .fullyConnectToNextLayer()
                // TODO uncomment for ablation study with fixed topology
//                .hiddenLayer(10)
//                .fullyConnectToNextLayer()
                .outputLayer()
                .finish();

        this.population = PopulationFactory.create(configuration, baseNetwork, getRNG());
    }

    private NeuralNetwork createRandomBaseNetwork() {
        int numberOfHiddenNodes = getRNG().getNextInt(0, 10);

        NeuralNetwork nn;
        if (numberOfHiddenNodes == 0) {
            nn = NeuronBasedNeuralNetworkBuilder.buildWithConfiguration(ConfigurationHelper.extractEmbeddedConfiguration(getOptimizationConfiguration(), NeuralNetworkConfiguration.class, OptimizationConfiguration.NEURAL_NETWORK_CONFIGURATION))
                    .implicitBias()
                    .inputLayer()
                    .outputLayer()
                    .finish();
        } else {
            nn = NeuronBasedNeuralNetworkBuilder.buildWithConfiguration(ConfigurationHelper.extractEmbeddedConfiguration(getOptimizationConfiguration(), NeuralNetworkConfiguration.class, OptimizationConfiguration.NEURAL_NETWORK_CONFIGURATION))
                    .implicitBias()
                    .inputLayer()
                    .hiddenLayer(numberOfHiddenNodes)
                    .outputLayer()
                    .finish();
        }

        List<NeuronID> possibleSources = IntStream.range(0, nn.getDepth())
                .mapToObj(nn::getNeuronsOfLayer)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // input neurons can be sources but not targets
        List<NeuronID> possibleTargets = possibleSources.stream()
                .filter(Predicate.not(nn::isInputNeuron))
                .collect(Collectors.toList());

        double connectionCountMean = 1 + (possibleTargets.size() - 1) * 0.2;
        double connectionCountDeviation = possibleTargets.size() * 0.4;

        for (NeuronID possibleSource : possibleSources) {
            int connectionCount = (int) Math.round(getRNG().getNormalDistributedValue(connectionCountMean, connectionCountDeviation));
            connectionCount = Math.max(connectionCount, 0);
            getRNG().shuffleCollection(possibleTargets);
            for (NeuronID possibleTarget : possibleTargets) {
                if (connectionCount-- <= 0) {
                    break;
                }
                nn.modify().addConnection(possibleSource, possibleTarget, 0);
                connectionCount--;
            }
        }

        return nn;
    }

    @Override
    public void resetAndRestart() {
        super.resetAndRestart();

        this.runState.execute(t -> t.resetValue(PacoRunState.CONNECTION_WEIGHTS_SCATTERED));

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
                .max(Comparator.comparingDouble(PacoAnt::getGeneralizationCapability)
                        .thenComparingDouble(PacoAnt::getFitness))
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
        this.runState.execute(s -> {
            s.resetValue(PacoRunState.CONNECTION_WEIGHTS_SCATTERED);
            s.resetValue(PacoRunState.USED_GROUPS);
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
