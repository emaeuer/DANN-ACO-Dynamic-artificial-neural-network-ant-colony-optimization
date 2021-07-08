package de.emaeuer.environment;

public interface AgentController {

    double[] getAction(double[] agentData);

    double getScore();

    void setScore(double score);

    double getGeneralizationCapability();

    void setGeneralizationCapability(double value);

    double getMaxAction();

    double getMinAction();

    AgentController copy();

}
