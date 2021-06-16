package de.emaeuer.environment;

public interface AgentController {

    double[] getAction(double[] agentData);

    void setScore(double score);

    double getMaxAction();

    double getMinAction();

}
