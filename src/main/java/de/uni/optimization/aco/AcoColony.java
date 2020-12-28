package de.uni.optimization.aco;

import de.uni.ann.NeuralNetwork;
import de.uni.optimization.aco.pheromone.CompositePheromoneMatrix;
import de.uni.optimization.aco.pheromone.LayerPheromoneMatrix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class AcoColony {

    private static final AtomicInteger NEXT_COLONY_NUMBER = new AtomicInteger(0);

    private final int colonyNumber = NEXT_COLONY_NUMBER.getAndIncrement();

    private int size;

    private CompositePheromoneMatrix pheromoneMatrix;

    private double maxFitness = 0;
    private Ant bestAnt = null;
    private final List<Ant> currentAnts = new ArrayList<>();

    private NeuralNetwork nn;

    public AcoColony(NeuralNetwork nn, int colonySize) {
        this.size = colonySize;

        this.nn = nn;
        nn.randomize();

        this.pheromoneMatrix = CompositePheromoneMatrix.buildForNeuralNetwork(this.nn);
    }

    public List<Ant> generateSolutions() {
        this.currentAnts.clear();

        if (this.bestAnt != null) {
            this.currentAnts.add(this.bestAnt);
        }

        IntStream.range(0, this.size - this.currentAnts.size())
                // create ants
                .mapToObj(i -> new Ant(this.nn.copy(), this.pheromoneMatrix))
                .peek(ant -> ant.setColonyNumber(this.colonyNumber))
                .peek(Ant::walk)
                .forEach(this.currentAnts::add);
        return this.currentAnts;
    }

    public void update() {
        // all update according to the average
        this.currentAnts.stream()
                .max(Comparator.comparingDouble(Ant::getFitness))
                .ifPresent(ant -> {
                    this.maxFitness = ant.getFitness();
                    this.bestAnt = ant;
                    this.bestAnt.setFitness(0);
                    this.bestAnt.setColonyBest(true);
                    this.nn = bestAnt.getNeuralNetwork();
                    this.pheromoneMatrix.updateSolution(ant.getSolution(), ant.getFitness() - 0);
                });

        // dissipate pheromone
        this.pheromoneMatrix.stream()
                .forEach(LayerPheromoneMatrix::dissipate);
    }

    public void takeSolutionOf(AcoColony other) {
        System.out.println("overtake");
        this.pheromoneMatrix = CompositePheromoneMatrix.buildForNeuralNetwork(other.nn);
        this.nn = other.nn;
        this.bestAnt = null;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public CompositePheromoneMatrix getPheromoneMatrix() {
        return this.pheromoneMatrix;
    }

    public double getFitness() {
        return this.maxFitness;
    }
}
