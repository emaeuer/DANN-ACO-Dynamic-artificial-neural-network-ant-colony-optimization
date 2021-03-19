package de.emaeuer.optimization.util;

public class ProgressionHandler {

    private int currentIteration = 0;
    private int numberOfIterationsToCheck;
    private int iterationWithLastIncrease = 0;

    private double fitnessOfLastIterationWithIncrease = 0;
    private double progressThreshold;

    public ProgressionHandler(int numberOfIterationsToCheck, double progressThreshold) {
        this.numberOfIterationsToCheck = numberOfIterationsToCheck;
        this.progressThreshold = progressThreshold;
    }

    public void addFitnessScore(double fitness) {
        currentIteration++;
        if (fitness > fitnessOfLastIterationWithIncrease + (1 + progressThreshold)) {
            this.fitnessOfLastIterationWithIncrease = fitness;
            this.iterationWithLastIncrease = currentIteration;
        }
    }

    public boolean doesStagnate() {
        return currentIteration > iterationWithLastIncrease + numberOfIterationsToCheck;
    }

    /**
     * Prevents a stagnation detection for the next numberOfIterationsToCheck iterations
     */
    public void resetProgression() {
        iterationWithLastIncrease = currentIteration;
        fitnessOfLastIterationWithIncrease = 0;
    }

}
