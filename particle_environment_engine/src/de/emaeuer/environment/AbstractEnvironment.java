package de.emaeuer.environment;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.factory.OptimizationMethodFactory;
import de.emaeuer.state.StateHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractEnvironment {

    private final double width = 800;
    private final double height = 800;

    private final List<AbstractElement> particles = new ArrayList<>();

    private final BiConsumer<AbstractElement, AbstractEnvironment> borderStrategy;

    private OptimizationMethod optimization;

    private double maxFitnessScore = 1000;

    public AbstractEnvironment(BiConsumer<AbstractElement, AbstractEnvironment> borderStrategy, ConfigurationHandler<EnvironmentConfiguration> configuration, StateHandler<OptimizationState> state) {
        this.borderStrategy = borderStrategy;

        initialize(configuration, state);

        initializeParticles();
    }

    protected void initialize(ConfigurationHandler<EnvironmentConfiguration> configuration, StateHandler<OptimizationState> state) {
        //noinspection unchecked
        ConfigurationHandler<OptimizationConfiguration> optimizationConfig = configuration.getValue(EnvironmentConfiguration.OPTIMIZATION_CONFIGURATION, ConfigurationHandler.class);
        this.optimization = OptimizationMethodFactory.createMethodForConfig(optimizationConfig, state);

        this.maxFitnessScore = optimizationConfig.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);
    }

    protected abstract void initializeParticles();

    public abstract void restart();

    public void update() {
        if (isRestartNecessary()) {
            return;
        }

        this.particles.stream()
                .peek(AbstractElement::step)
                .forEach(this::checkBorderCase);
    }

    private void checkBorderCase(AbstractElement particle) {
        if (this.borderStrategy != null) {
            this.borderStrategy.accept(particle, this);
        }
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public List<AbstractElement> getParticles() {
        return particles;
    }

    protected OptimizationMethod getOptimization() {
        return optimization;
    }

    public boolean isOptimizationFinished() {
        return this.optimization.isOptimizationFinished();
    }

    public abstract boolean isRestartNecessary();

    public double getMaxFitness() {
        return this.optimization.getBestFitness();
    }

    public double getFitnessThreshold() {
        return this.optimization.getFitnessThreshold();
    }

    public int getNumberOfEvaluations() {
        return this.optimization.getEvaluationCounter();
    }

    public int getEvaluationThreshold() {
        return this.optimization.getEvaluationThreshold();
    }

    protected double getMaxFitnessScore() {
        return this.maxFitnessScore;
    }

    public int getNumberOfRuns() {
        return this.optimization.getRunCounter();
    }

    public int getMaxNumberOfRuns() {
        return this.optimization.getMaxNumberOfRuns();
    }
}
