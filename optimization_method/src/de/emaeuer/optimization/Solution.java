package de.emaeuer.optimization;

import org.apache.commons.math3.linear.RealVector;

public interface Solution {

    public abstract RealVector process(RealVector input);

    public double getFitness();

    public void setFitness(double fitness);
}
