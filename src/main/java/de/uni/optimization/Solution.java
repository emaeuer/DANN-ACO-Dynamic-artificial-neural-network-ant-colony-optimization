package de.uni.optimization;

import org.apache.commons.math3.linear.RealVector;

public abstract class Solution {

    private double fitness = 0;

    public abstract RealVector process(RealVector input);

    public double getFitness() {
        return this.fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}
