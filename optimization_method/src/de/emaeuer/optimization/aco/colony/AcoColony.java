package de.emaeuer.optimization.aco.colony;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.configuration.ConfigurationVariablesBuilder;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.aco.AcoAnt;
import de.emaeuer.optimization.aco.Decision;
import de.emaeuer.optimization.aco.configuration.AcoConfiguration;
import de.emaeuer.optimization.aco.configuration.AcoParameter;
import de.emaeuer.optimization.aco.pheromone.PheromoneMatrix;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.util.ProgressionHandler;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.aco.configuration.AcoConfiguration.*;
import static de.emaeuer.optimization.aco.configuration.AcoParameter.NUMBER_OF_DECISIONS;
import static de.emaeuer.optimization.aco.configuration.AcoParameter.PHEROMONE;

public class AcoColony {

    private final static Logger LOG = LogManager.getLogger(AcoColony.class);

    private final int colonyNumber;

    private PheromoneMatrix pheromoneMatrix;
    private NeuralNetwork neuralNetwork;

    private final List<AcoAnt> ants = new ArrayList<>();

    private double bestScore = 0;
    private AcoAnt currentBest;

    private final ConfigurationHandler<AcoConfiguration> configuration;

    private final ProgressionHandler stagnationChecker;

    public AcoColony(NeuralNetwork neuralNetwork, ConfigurationHandler<OptimizationConfiguration> configuration, int colonyNumber) {
        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, AcoConfiguration.class, OptimizationConfiguration.OPTIMIZATION_CONFIGURATION);
        this.colonyNumber = colonyNumber;

        this.neuralNetwork = neuralNetwork;
        this.pheromoneMatrix = PheromoneMatrix.buildForNeuralNetwork(neuralNetwork, this.configuration);

        Map<String, Double> variables = ConfigurationVariablesBuilder.<AcoParameter>build()
                .with(PHEROMONE, 0)
                .with(NUMBER_OF_DECISIONS, 0)
                .getVariables();

        this.stagnationChecker = new ProgressionHandler(
                configuration.getValue(OptimizationConfiguration.OPTIMIZATION_PROGRESSION_ITERATIONS, Integer.class, variables),
                configuration.getValue(OptimizationConfiguration.OPTIMIZATION_PROGRESSION_THRESHOLD, Double.class, variables));
    }

    public List<AcoAnt> nextIteration() {
        this.ants.clear();

        if (this.currentBest != null) {
            this.ants.add(this.currentBest);
        }

        // generate solutions
        IntStream.range(this.ants.size(), configuration.getValue(ACO_COLONY_SIZE, Integer.class))
                .mapToObj(i -> new AcoAnt(this))
                .peek(AcoAnt::generateSolution)
                .forEach(this.ants::add);

        return this.ants;
    }

    public void updateSolutions() {
        // TODO may replace by lambda function to change the update strategy flexible

        // simplest update strategy --> only best updates (take its neural network and update its solution)
        Collections.shuffle(this.ants); // if multiple ants have the same fitness the selected one should be randomly selected
        AcoAnt best = this.ants.stream()
                .max(Comparator.comparingDouble(AcoAnt::getFitness))
                .orElseThrow(() -> new IllegalStateException("Iteration produced no solutions")); // shouldn't happen

        this.currentBest = best;
        // solution haas to be applied to the existing neural network to keep references to the pheromone matrix valid
        applySolutionToNeuralNetwork(best.getSolution());
        this.pheromoneMatrix.updatePheromone(best.getSolution());

        int antID = this.colonyNumber * this.ants.size() + this.ants.indexOf(best);
        LOG.info("Ant {} updated in colony {} with a fitness of {}", antID, getColonyNumber(), best.getFitness());

        // check for stagnation of fitness and mutate
        checkAndHandleStagnation();
    }

    private void applySolutionToNeuralNetwork(List<Decision> solution) {
        NeuronID currentNeuron = solution.get(0).neuronID();
        for (Decision decision : solution.subList(1, solution.size())) {
            NeuronID target = decision.neuronID();
            this.neuralNetwork.setBiasOfNeuron(target, decision.biasValue());

            if (this.neuralNetwork.neuronHasConnectionTo(currentNeuron, target)) {
                this.neuralNetwork.setWeightOfConnection(currentNeuron, target, decision.weightValue());
            } else {
                LOG.info("Adding new connection from {} to {} to neural network of colony {}", currentNeuron, target, this.colonyNumber);
                this.neuralNetwork.modify().addConnection(currentNeuron, target, decision.weightValue());
                this.pheromoneMatrix.modify().addConnection(currentNeuron, target);
            }

            currentNeuron = target;
        }
    }

    private void checkAndHandleStagnation() {
        this.stagnationChecker.addFitnessScore(getCurrentFitness());
        if (this.stagnationChecker.doesStagnate()) {
            LOG.info("Detected stagnation of fitness in ACO-Colony {}", getColonyNumber());
            mutateNeuralNetwork();
            this.stagnationChecker.resetProgression();
        }
    }

    public DoubleSummaryStatistics getIterationStatistic() {
        DoubleSummaryStatistics statistic = this.ants.stream()
                .mapToDouble(AcoAnt::getFitness)
                .summaryStatistics();

        this.bestScore = Double.max(statistic.getMax(), this.bestScore);

        return statistic;
    }

    public void takeSolutionOf(AcoColony other) {
        this.neuralNetwork = other.neuralNetwork.copy();
        this.pheromoneMatrix = PheromoneMatrix.buildForNeuralNetwork(this.neuralNetwork, configuration);

        // mutate neural network
        mutateNeuralNetwork();

        this.ants.clear();
        this.currentBest = null;
    }

    private void mutateNeuralNetwork() {
        IntStream.range(0, this.neuralNetwork.getDepth())
                .mapToObj(i -> this.neuralNetwork.getNeuronsOfLayer(i)) // stream of all layers
                .flatMap(List::stream) // stream of all neurons of this layer
                .flatMap(n ->
                    this.neuralNetwork.getOutgoingConnectionsOfNeuron(n)
                            .stream()
                            .map(target -> new SimpleEntry<>(n, target)))
                .filter(c -> Math.random() < calculateSplitProbability(c)) // select random connections
                .peek(c -> LOG.info("Modifying neural network and pheromone matrix of ACO-Colony {} by splitting connection between {} and {}", getColonyNumber(), c.getKey(), c.getValue()))
                .peek(c -> this.neuralNetwork.modify().splitConnection(c.getKey(), c.getValue()))
                .forEach(c -> this.pheromoneMatrix.modify().splitConnection(c.getKey(), c.getValue(), this.neuralNetwork.modify().getLastModifiedNeuron()));

        this.currentBest = null;
    }

    private double calculateSplitProbability(Entry<NeuronID, NeuronID> connection) {
        Map<String, Double> variables = ConfigurationVariablesBuilder.<AcoParameter>build()
                .with(PHEROMONE, this.pheromoneMatrix.getWeightPheromoneOfConnection(connection.getKey(), connection.getValue()))
                .with(NUMBER_OF_DECISIONS, 1)
                .getVariables();

        return configuration.getValue(ACO_CONNECTION_SPLIT_PROBABILITY, Double.class, variables);
    }

    public NeuralNetwork getNeuralNetwork() {
        return neuralNetwork;
    }

    public PheromoneMatrix getPheromoneMatrix() {
        return pheromoneMatrix;
    }

    public int getColonyNumber() {
        return colonyNumber;
    }

    public double getCurrentFitness() {
        return getIterationStatistic().getMax();
    }

    public double getBestScore() {
        return bestScore;
    }

    public ConfigurationHandler<AcoConfiguration> getConfiguration() {
        return this.configuration;
    }

    public Solution getBestAnt() {
        return this.currentBest;
    }
}
