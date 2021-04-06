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
    public int getAction(double[] agentData) {
        double[] result = brain.process(new ArrayRealVector(agentData)).toArray();

        if (result[0] > 0.5) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void setScore(double score) {
        brain.setFitness(score);
    }

}
