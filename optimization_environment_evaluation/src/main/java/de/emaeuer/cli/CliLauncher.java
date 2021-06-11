package de.emaeuer.cli;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.evaluation.OptimizationEnvironmentHandler;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.state.StateHandler;
import javafx.application.Platform;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;

public class CliLauncher {

    private OptimizationEnvironmentHandler optimization = new OptimizationEnvironmentHandler();

    private final StateHandler<OptimizationState> optimizationState = new StateHandler<>(OptimizationState.class);
    private final ConfigurationHandler<OptimizationConfiguration> optimizationConfiguration = new ConfigurationHandler<>(OptimizationConfiguration.class);
    private final ConfigurationHandler<EnvironmentConfiguration> environmentConfiguration = new ConfigurationHandler<>(EnvironmentConfiguration.class);

    public static void main(String[] args) {
        CliParameter parameters = new CliParameter();
        new CommandLine(parameters).parseArgs(args);

        Platform.startup(() -> {});

        CliLauncher launcher = new CliLauncher(parameters);
        launcher.run();

        Platform.exit();
    }

    public CliLauncher(CliParameter parameters) {
        initEnvironmentConfiguration(parameters.getEnvironmentConfig());
        initOptimizationConfiguration(parameters.getOptimizationConfig(), parameters);
    }

    private void initEnvironmentConfiguration(File file) {
        if (file.exists()) {
            this.environmentConfiguration.importConfig(file);
        }
    }

    private void initOptimizationConfiguration(File file, CliParameter parameters) {
        if (file.exists()) {
            this.optimizationConfiguration.importConfig(file);
        }

        if (OptimizationMethodNames.PACO.name().equals(optimizationConfiguration.getValue(OptimizationConfiguration.METHOD_NAME, String.class))) {
            ConfigurationHandler<PacoConfiguration> config = ConfigurationHelper.extractEmbeddedConfiguration(this.optimizationConfiguration, PacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
            applyParametersToConfig(config, parameters);
        }
    }

    private void applyParametersToConfig(ConfigurationHandler<PacoConfiguration> config, CliParameter parameters) {
        Optional.ofNullable(parameters.getPopulationSize()).ifPresent(v -> config.setValue(PacoConfiguration.POPULATION_SIZE, v));
        Optional.ofNullable(parameters.getNumberOfUpdates()).ifPresent(v -> config.setValue(PacoConfiguration.UPDATES_PER_ITERATION, v));
        Optional.ofNullable(parameters.getAntsPerIteration()).ifPresent(v -> config.setValue(PacoConfiguration.ANTS_PER_ITERATION, v));
        Optional.ofNullable(parameters.getDeviationFunction()).ifPresent(v -> config.setValue(PacoConfiguration.DEVIATION_FUNCTION, v));
        Optional.ofNullable(parameters.getPopulationStrategy()).ifPresent(v -> config.setValue(PacoConfiguration.UPDATE_STRATEGY, v));
        Optional.ofNullable(parameters.getChangeProbability()).ifPresent(v -> config.setValue(PacoConfiguration.DYNAMIC_PROBABILITY, v));
        Optional.ofNullable(parameters.getPheromoneValue()).ifPresent(v -> config.setValue(PacoConfiguration.PHEROMONE_VALUE, v));
        Optional.ofNullable(parameters.getSpitThreshold()).ifPresent(v -> config.setValue(PacoConfiguration.SPLIT_THRESHOLD, v));
        Optional.ofNullable(parameters.isElitism()).ifPresent(v -> config.setValue(PacoConfiguration.ELITISM, v));
        Optional.ofNullable(parameters.isNeuronIsolation()).ifPresent(v -> config.setValue(PacoConfiguration.ENABLE_NEURON_ISOLATION, v));
    }

    private void run() {
        this.optimization.optimizationStateProperty().set(this.optimizationState);
        this.optimization.optimizationConfigurationProperty().set(this.optimizationConfiguration);
        this.optimization.environmentConfigurationProperty().set(this.environmentConfiguration);
        this.optimization.setUpdateDelta(0);

        this.optimization.initialize();
        this.optimization.run();
    }
}
