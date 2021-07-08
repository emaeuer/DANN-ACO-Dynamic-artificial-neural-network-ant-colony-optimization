package de.emaeuer.optimization.neat.mapping;

import com.anji.integration.*;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.Solution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jgap.Chromosome;

public class ChromosomeSolutionMapping implements Solution {

    private final Chromosome chromosome;
    private final AnjiActivator activator;

    private final double maxFitness;
    private double fitness = 0;
    private double generalizationCapability = 0;

    private NeuralNetwork mappedNN = null;

    public ChromosomeSolutionMapping(Chromosome chromosome, Activator activator, double maxFitness) {
        this.chromosome = chromosome;
        this.activator = (AnjiActivator) activator;
        this.maxFitness = maxFitness;
    }

    @Override
    public RealVector process(RealVector input) {
        // add bias as input which is always one
        input = new ArrayRealVector(new double[] {1}).append(input);
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

        // anji requires int as fitness value --> for maximal precision all positive integers are used
        int chromosomeFitness = Double.valueOf(fitness * (Integer.MAX_VALUE / maxFitness)).intValue();
        this.chromosome.setFitnessValue(chromosomeFitness);
    }

    @Override
    public NeuralNetwork getNeuralNetwork() {
        if (mappedNN == null) {
            // lazy initialization
            this.mappedNN = AnjiNetToNeuralNetwork.mapToNeuralNetwork(this.activator);
        }
        return this.mappedNN;
    }

    @Override
    public double getGeneralizationCapability() {
        return generalizationCapability;
    }

    @Override
    public void setGeneralizationCapability(double value) {
        this.generalizationCapability = value;
    }

    @Override
    public Solution copy() {
        return ChromosomeSolutionMapper.map(new Chromosome(this.chromosome.cloneMaterial(), this.chromosome.getId()), this.maxFitness);
    }
}
