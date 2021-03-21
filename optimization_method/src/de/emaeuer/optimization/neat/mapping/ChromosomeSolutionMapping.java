package de.emaeuer.optimization.neat.mapping;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.Solution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jgap.Chromosome;

public class ChromosomeSolutionMapping implements Solution {

    private final Chromosome chromosome;
    private final Activator activator;

    private double fitness = 0;

    public ChromosomeSolutionMapping(Chromosome chromosome, Activator activator) {
        this.chromosome = chromosome;
        this.activator = activator;
    }

    @Override
    public RealVector process(RealVector input) {
        double[] result = activator.next(input.toArray());
        return new ArrayRealVector(result);
    }

    @Override
    public double getFitness() {
        return this.fitness;
    }

    @Override
    public void setFitness(double fitness) {
        this.fitness = fitness;
        // set chromosome fitness value with a precision of 10^2
        this.chromosome.setFitnessValue(Double.valueOf(fitness).intValue() * 100);
    }

    @Override
    public NeuralNetwork getNeuralNetwork() {
        return null;
    }
}