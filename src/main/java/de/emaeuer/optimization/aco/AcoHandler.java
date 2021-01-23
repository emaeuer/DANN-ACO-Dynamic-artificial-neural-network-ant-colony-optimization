package de.emaeuer.optimization.aco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AcoHandler implements OptimizationMethod {

    private final NeuralNetwork nn;

    private final List<AcoColony> colonies = new ArrayList<>();

    private int generationCounter = 0;

    public AcoHandler(NeuralNetwork network, int numberOfColonies, int colonySize) {
        this.nn = network.copy();

        for (int i = 0; i < numberOfColonies; i++) {
            colonies.add(new AcoColony(this.nn, colonySize));
        }
    }

    @Override
    public List<Solution> generateSolutions() {
        this.generationCounter++;
        return this.colonies.stream()
                .map(AcoColony::generateSolutions)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void update() {
        this.colonies.forEach(AcoColony::update);

        AcoColony worst = this.colonies.get(0);
        AcoColony best = this.colonies.get(0);

        for (AcoColony colony: this.colonies) {
            worst = colony.getFitness() < worst.getFitness() ? colony : worst;
            best = colony.getFitness() > best.getFitness() ? colony : best;
        }

        System.out.printf("Generation %d [ %.1f : %.1f ]%n", this.generationCounter, worst.getFitness(), best.getFitness());

        // best is a lot better than worst --> worst gets solution of best
        if (worst.getFitness() * 10 < best.getFitness()) {
            worst.takeSolutionOf(best);
        }
    }


}
