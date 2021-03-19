package de.emaeuer.optimization.paco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.Solution;
import org.apache.commons.math3.linear.RealVector;

public class PacoAnt implements Solution {

    private double fitness = 0;

    private final NeuralNetwork brain;

    public PacoAnt(NeuralNetwork brain) {
        this.brain = brain;
    }

    @Override
    public RealVector process(RealVector input) {
        return this.brain.process(input);
    }

    public double getFitness() {
        return this.fitness;
    }

    @Override
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public NeuralNetwork getNeuralNetwork() {
        return brain;
    }
}
