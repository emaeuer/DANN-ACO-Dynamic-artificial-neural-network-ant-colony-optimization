package de.emaeuer.aco.colony;

import de.emaeuer.aco.AcoHandler;
import de.emaeuer.aco.Ant;
import de.emaeuer.aco.Decision;
import de.emaeuer.aco.pheromone.PheromoneMatrix;
import de.emaeuer.aco.util.ProgressionHandler;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AcoColony {

    private static final AtomicInteger NEXT_COLONY_NUMBER = new AtomicInteger(0);

    private final int colonyNumber = NEXT_COLONY_NUMBER.getAndIncrement();

    private PheromoneMatrix pheromoneMatrix;
    private NeuralNetwork neuralNetwork;

    private int colonySize;

    private final List<Ant> ants = new ArrayList<>();

    private Ant currentBest;

    // TODO extract configuration to configuration class
    private final ProgressionHandler stagnationChecker = new ProgressionHandler(5, 0.2);

    public AcoColony(NeuralNetwork neuralNetwork, int colonySize) {
        this.neuralNetwork = neuralNetwork;
        this.pheromoneMatrix = PheromoneMatrix.buildForNeuralNetwork(neuralNetwork);

        this.colonySize = colonySize;
    }

    public List<Ant> nextIteration() {
        this.ants.clear();

        if (this.currentBest != null) {
            this.ants.add(this.currentBest);
        }

        // generate solutions
        IntStream.range(this.ants.size(), this.colonySize)
                .mapToObj(i -> new Ant(this))
                .peek(Ant::generateSolution)
                .forEach(this.ants::add);

        return this.ants;
    }

    public void updateSolutions() {
        // TODO may replace by lambda function to change the update strategy flexible

        // simplest update strategy --> only best updates (take its neural network and update its solution)
        Ant best = this.ants.stream()
                .max(Comparator.comparingDouble(Ant::getFitness))
                .orElseThrow(() -> new IllegalStateException("Iteration produced no solutions")); // shouldn't happen

        this.currentBest = best;
        applySolutionToNeuralNetwork(best.getSolution());
        this.pheromoneMatrix.updatePheromone(best.getSolution());

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
        this.pheromoneMatrix = PheromoneMatrix.buildForNeuralNetwork(this.neuralNetwork);

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

        connections.stream()
                .filter(c -> Math.random() < AcoHandler.SPLIT_PROBABILITY)
                .peek(c -> System.out.printf("Split connection from %s to %s in colony %d\n", c.getKey(), c.getValue(), this.colonyNumber))
                .peek(c -> this.neuralNetwork.modify().splitConnection(c.getKey(), c.getValue()))
                .forEach(c -> this.pheromoneMatrix.modify().splitConnection(c.getKey(), c.getValue(), this.neuralNetwork.modify().getLastModifiedNeuron()));

        this.currentBest = null;
    }

    public int getColonySize() {
        return colonySize;
    }

    public void setColonySize(int colonySize) {
        this.colonySize = colonySize;
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
