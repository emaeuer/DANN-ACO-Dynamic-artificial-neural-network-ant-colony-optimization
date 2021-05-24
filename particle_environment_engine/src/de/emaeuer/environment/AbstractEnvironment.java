package de.emaeuer.environment;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.elements.AbstractElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractEnvironment {

    private final double width = 800;
    private final double height = 800;

    private final List<AbstractElement> agents = Collections.synchronizedList(new ArrayList<>());

    private final BiConsumer<AbstractElement, AbstractEnvironment> borderStrategy;

    private double maxFitnessScore = 1000;

    public AbstractEnvironment(BiConsumer<AbstractElement, AbstractEnvironment> borderStrategy, ConfigurationHandler<EnvironmentConfiguration> configuration) {
        this.borderStrategy = borderStrategy;

        initialize(configuration);
    }

    protected void initialize(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        this.maxFitnessScore = configuration.getValue(EnvironmentConfiguration.MAX_FITNESS_SCORE, Double.class);
    }

    public void setControllers(List<AgentController> controllers) {
        restart();
        initializeParticles(controllers);
    }

    public void step() {
        this.agents.stream()
                .peek(AbstractElement::step)
                .forEach(this::checkBorderCase);
    }

    protected abstract void initializeParticles(List<AgentController> controllers);

    public abstract void restart();

    private void checkBorderCase(AbstractElement particle) {
        if (this.borderStrategy != null) {
            this.borderStrategy.accept(particle, this);
        }
    }

    public List<AbstractElement> getAdditionalEnvironmentElements() {
        return Collections.synchronizedList(new ArrayList<>());
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public List<AbstractElement> getAgents() {
        return agents;
    }

    public abstract boolean allAgentsFinished();

    protected double getMaxFitnessScore() {
        return this.maxFitnessScore;
    }
}
