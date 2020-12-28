package de.uni.optimization.ga;

import de.uni.ann.NeuralNetwork;
import de.uni.optimization.OptimizationMethod;
import de.uni.optimization.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeneticAlgorithm implements OptimizationMethod {

    private static final Random GENERATOR = new Random();

    private final List<Brain> solutions = new ArrayList<>();

    public GeneticAlgorithm(NeuralNetwork network, int populationSize) {
        IntStream.range(0, populationSize)
                .mapToObj(i -> new Brain(network.copy()))
                .forEach(this.solutions::add);
    }

    @Override
    public List<Solution> generateSolutions() {
        return new ArrayList<>(this.solutions);
    }

    @Override
    public void update() {
        int populationSize = solutions.size();
        List<Brain> top5Percent = this.solutions.stream()
                .sorted((b1, b2) -> Double.compare(b2.getFitness(), b1.getFitness()))
                .limit(Math.max(populationSize / 20, 5))
                .collect(Collectors.toList());

        this.solutions.clear();
        while (this.solutions.size() < populationSize) {
            this.solutions.add(new Brain(chooseOneRandom(top5Percent), chooseOneRandom(top5Percent)));
        }
    }

    private static Brain chooseOneRandom(List<Brain> brains) {
        double fitnessSum = brains.stream()
                .mapToDouble(Brain::getFitness)
                .sum();

        double cumulatedFitnessSum = 0;
        double selectionThreshold = Math.random() * fitnessSum;
        for (Brain brain : brains) {
            cumulatedFitnessSum += brain.getFitness();
            if (selectionThreshold <= cumulatedFitnessSum) {
                return brain;
            }
        }
        // return best if nothing was selected before (should not happen)
        return brains.get(0);
    }
}
