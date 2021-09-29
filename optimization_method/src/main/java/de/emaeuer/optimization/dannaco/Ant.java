package de.emaeuer.optimization.dannaco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.TopologyData;
import org.apache.commons.math3.linear.RealVector;

public class Ant implements Solution {

    private double fitness = 0;
    private double generalizationCapability = 0;

    private final TopologyData solution;

    public Ant(NeuralNetwork brain, int topologyGroupID) {
        this.solution = new TopologyData(brain, topologyGroupID);
    }

    public Ant(TopologyData topology) {
        this.solution = topology;
    }

    @Override
    public RealVector process(RealVector input) {
        return this.solution.getInstance().process(input);
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
        return solution.getInstance();
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
        return new Ant(this.solution.copy());
    }

    public TopologyData getTopologyData() {
        return this.solution;
    }
}
