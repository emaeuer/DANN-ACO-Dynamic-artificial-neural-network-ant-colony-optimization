package de.uni.ann;

import org.apache.commons.math3.linear.RealMatrix;

public class Connection {

    private final RealMatrix weightMatrix;

    private final Neuron start;
    private final Neuron end;

    /**
     * Receives a reference to the complete weight matrix so that a change of the weight automatically changes the weight in the matrix
     *
     * @param start The start neuron
     * @param end The end neuron
     * @param weights Reference to the weight matrix of the layer which contains the end neuron
     */
    public Connection(Neuron start, Neuron end, RealMatrix weights) {
        this.start = start;
        this.end = end;
        this.weightMatrix = weights;
    }

    public double getWeight() {
        return this.weightMatrix.getEntry(this.end.getIndexInLayer(), this.start.getIndexInLayer());
    }

    public void setWeight(double weight) {
        this.weightMatrix.setEntry(this.end.getIndexInLayer(), this.start.getIndexInLayer(), Math.min(Math.max(weight, -1), 1));
    }

    public Neuron getEnd() {
        return end;
    }

    public Neuron getStart() {
        return start;
    }
}
