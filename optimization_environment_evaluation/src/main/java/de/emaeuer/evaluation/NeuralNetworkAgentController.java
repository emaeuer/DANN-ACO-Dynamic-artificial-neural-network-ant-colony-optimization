package de.emaeuer.evaluation;

import de.emaeuer.environment.AgentController;
import de.emaeuer.optimization.Solution;
import org.apache.commons.math3.linear.ArrayRealVector;

public class NeuralNetworkAgentController implements AgentController {

    private final Solution brain;

    public NeuralNetworkAgentController(Solution brain) {
        this.brain = brain;
    }

    @Override
    public double[] getAction(double[] agentData) {
        return brain.process(new ArrayRealVector(agentData)).toArray();
    }

    @Override
    public double getScore() {
        return this.brain.getFitness();
    }

    @Override
    public void setScore(double score) {
        brain.setFitness(score);
    }

    @Override
    public double getGeneralizationCapability() {
        return brain.getGeneralizationCapability();
    }

    @Override
    public void setGeneralizationCapability(double value) {
        this.brain.setGeneralizationCapability(value);
    }

    @Override
    public double getMaxAction() {
        return this.brain.getNeuralNetwork().getMaxActivation();
    }

    @Override
    public double getMinAction() {
        return this.brain.getNeuralNetwork().getMinActivation();
    }

    @Override
    public AgentController copy() {
        return new NeuralNetworkAgentController(brain.copy());
    }
}
