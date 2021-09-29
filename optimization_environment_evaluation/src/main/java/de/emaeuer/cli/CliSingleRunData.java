package de.emaeuer.cli;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.evaluation.EvaluationConfiguration;
import de.emaeuer.evaluation.OptimizationEnvironmentHandler;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;

public final class CliSingleRunData {
    private final OptimizationEnvironmentHandler optimization;
    private final StateHandler<OptimizationState> optimizationState;
    private final ConfigurationHandler<EvaluationConfiguration> configuration;

    public CliSingleRunData(OptimizationEnvironmentHandler optimization, StateHandler<OptimizationState> optimizationState, ConfigurationHandler<EvaluationConfiguration> configuration) {
        this.optimization = optimization;
        this.optimizationState = optimizationState;
        this.configuration = configuration;
    }

    public CliSingleRunData() {
        this(new OptimizationEnvironmentHandler(), new StateHandler<>(OptimizationState.class), new ConfigurationHandler<>(EvaluationConfiguration.class));

        this.optimization.setOptimizationState(this.optimizationState);
        this.optimization.setConfiguration(this.configuration);
        this.optimization.setUpdateDelta(0);
    }

    public void initialize() {
        this.optimization.initialize();
    }

    public OptimizationEnvironmentHandler getOptimization() {
        return optimization;
    }

    public StateHandler<OptimizationState> getOptimizationState() {
        return optimizationState;
    }

    public ConfigurationHandler<EvaluationConfiguration> getConfiguration() {
        return configuration;
    }
}
