package de.emaeuer.aco.colony;

import de.emaeuer.aco.AcoHandler;
import de.emaeuer.aco.Ant;
import de.emaeuer.aco.Decision;
import de.emaeuer.aco.configuration.AcoConfiguration;
import de.emaeuer.aco.configuration.AcoConfigurationKeys;
import de.emaeuer.aco.pheromone.PheromoneMatrix;
import de.emaeuer.aco.util.ProgressionHandler;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.emaeuer.aco.configuration.AcoConfigurationKeys.*;

public class AcoColony {

    private final static Logger LOG = LogManager.getLogger(AcoColony.class);

    private static final AtomicInteger NEXT_COLONY_NUMBER = new AtomicInteger(0);

    private final int colonyNumber = NEXT_COLONY_NUMBER.getAndIncrement();

    private PheromoneMatrix pheromoneMatrix;
    private NeuralNetwork neuralNetwork;

    private final List<Ant> ants = new ArrayList<>();

    private Ant currentBest;

    private final AcoConfiguration configuration;

    private final ProgressionHandler stagnationChecker;

    public AcoColony(NeuralNetwork neuralNetwork, AcoConfiguration configuration) {
        this.configuration = configuration;

        this.neuralNetwork = neuralNetwork;
        this.pheromoneMatrix = PheromoneMatrix.buildForNeuralNetwork(neuralNetwork, configuration);

        this.stagnationChecker = new ProgressionHandler(configuration.getValueAsInt(ACO_PROGRESSION_ITERATIONS), configuration.getValue(ACO_PROGRESSION_THRESHOLD));
    }

    public List<Ant> nextIteration() {
        this.ants.clear();

        if (this.currentBest != null) {
            this.ants.add(this.currentBest);
        }

        // generate solutions
        IntStream.range(this.ants.size(), configuration.getValueAsInt(AcoConfigurationKeys.ACO_COLONY_SIZE))
                .mapToObj(i -> new Ant(this))
                .peek(Ant::generateSolution)
                .forEach(this.ants::add);

        return this.ants;
    }

    public void updateSolutions() {
        // TODO may replace by lambda function to change the update strategy flexible

        // simplest update strategy --> only best updates (take its neural network and update its solution)
        Collections.shuffle(this.ants); // if multiple ants have the same fitness the selected one should be randomly selected
        Ant best = this.ants.stream()
                .max(Comparator.comparingDouble(Ant::getFitness))
                .orElseThrow(() -> new IllegalStateException("Iteration produced no solutions")); // shouldn't happen

        this.currentBest = best;
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
            this.neuralNetwork.setWeightOfConnection(currentNeuron, target, decision.weightValue());
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
        return this.ants.stream()
                .mapToDouble(Ant::getFitness)
                .summaryStatistics();
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
        List<Entry<NeuronID, NeuronID>> connections = IntStream.range(0, this.neuralNetwork.getDepth())
                .mapToObj(i -> this.neuralNetwork.getNeuronsOfLayer(i)) // stream of all layers
                .flatMap(List::stream) // stream of all neurons of this layer
                .flatMap(n ->
                    this.neuralNetwork.getOutgoingConnectionsOfNeuron(n)
                            .stream()
                            .map(target -> new SimpleEntry<>(n, target)))
                .collect(Collectors.toCollection(ArrayList::new));

        double splitProbability = configuration.getValue(ACO_CONNECTION_SPLIT_PROBABILITY);

        connections.stream()
                .filter(c -> Math.random() < splitProbability) // TODO extract split decision to method and add parameters
                .peek(c -> LOG.info("Modifying neural network and pheromone matrix of ACO-Colony {} by inserting connection from {} to {}", getColonyNumber(), c.getKey(), c.getValue()))
                .peek(c -> this.neuralNetwork.modify().splitConnection(c.getKey(), c.getValue()))
                .forEach(c -> this.pheromoneMatrix.modify().splitConnection(c.getKey(), c.getValue(), this.neuralNetwork.modify().getLastModifiedNeuron()));

        this.currentBest = null;
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
}
