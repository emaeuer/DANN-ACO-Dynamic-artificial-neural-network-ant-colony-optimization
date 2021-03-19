package de.emaeuer.optimization;

import de.emaeuer.ann.NeuralNetwork;
import org.apache.commons.math3.linear.RealVector;

public interface Solution {

    RealVector process(RealVector input);

    double getFitness();

    void setFitness(double fitness);

    NeuralNetwork getNeuralNetwork();
}
