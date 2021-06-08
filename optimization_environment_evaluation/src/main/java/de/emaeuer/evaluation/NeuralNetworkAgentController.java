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
    public void setScore(double score) {
        brain.setFitness(score);
    }

}
