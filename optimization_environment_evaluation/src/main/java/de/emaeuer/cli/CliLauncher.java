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
import de.emaeuer.state.value.DistributionStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public class CliLauncher {

    private static final Logger LOG = LogManager.getLogger(CliLauncher.class);

    private OptimizationEnvironmentHandler optimization = new OptimizationEnvironmentHandler();

    private final StateHandler<OptimizationState> optimizationState = new StateHandler<>(OptimizationState.class);
    private final ConfigurationHandler<OptimizationConfiguration> optimizationConfiguration = new ConfigurationHandler<>(OptimizationConfiguration.class);
    private final ConfigurationHandler<EnvironmentConfiguration> environmentConfiguration = new ConfigurationHandler<>(EnvironmentConfiguration.class);

    private long timeMillis = -1;

    public static void main(String[] args) {
        CliLauncher launcher = new CliLauncher(args);
        launcher.run();
        System.out.println((-1 * launcher.getCost()) + " [" + launcher.getTimeMillis() + "]");
    }

    public CliLauncher(String[] args) {
        LOG.debug("CLI-Call-Parameters: " + Arrays.toString(args));

        CliParameter parameters = new CliParameter();
        new CommandLine(parameters).parseArgs(args);

        initEnvironmentConfiguration(parameters.getEnvironmentConfig());
        initOptimizationConfiguration(parameters.getOptimizationConfig(), parameters);
    }

    public CliLauncher(String[] args, int seed) {
        LOG.debug("CLI-Call-Parameters: " + Arrays.toString(args));

        CliParameter parameters = new CliParameter();
        new CommandLine(parameters).parseArgs(args);

        initEnvironmentConfiguration(parameters.getEnvironmentConfig());
        initOptimizationConfiguration(parameters.getOptimizationConfig(), parameters);

        this.environmentConfiguration.setValue(EnvironmentConfiguration.SEED, seed);
        this.optimizationConfiguration.setValue(OptimizationConfiguration.SEED, seed);
    }

    private void initEnvironmentConfiguration(File file) {
        if (file != null && file.exists()) {
            this.environmentConfiguration.importConfig(file);
        }
    }

    private void initOptimizationConfiguration(File file, CliParameter parameters) {
        if (file != null && file.exists()) {
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
        Optional.ofNullable(parameters.getChangeProbability()).ifPresent(v -> config.setValue(PacoConfiguration.TOPOLOGY_PHEROMONE, v));
        Optional.ofNullable(parameters.getPheromoneValue()).ifPresent(v -> config.setValue(PacoConfiguration.CONNECTION_PHEROMONE, v));
        Optional.ofNullable(parameters.getSpitThreshold()).ifPresent(v -> config.setValue(PacoConfiguration.SPLIT_PROBABILITY, v));
        Optional.ofNullable(parameters.isElitism()).ifPresent(v -> config.setValue(PacoConfiguration.ELITISM, v));
        Optional.ofNullable(parameters.isNeuronIsolation()).ifPresent(v -> config.setValue(PacoConfiguration.ENABLE_NEURON_ISOLATION, v));
    }

    public void run() {
        this.optimization.setOptimizationState(this.optimizationState);
        this.optimization.setOptimizationConfiguration(this.optimizationConfiguration);
        this.optimization.setEnvironmentConfiguration(this.environmentConfiguration);
        this.optimization.setUpdateDelta(0);

        this.optimization.initialize();

        long startTime = System.nanoTime();
        this.optimization.run();
        this.timeMillis = (System.nanoTime() - startTime) / 1000;
    }

    public double getCost() {
        double averageFitness = ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.FITNESS_DISTRIBUTION)).getMean();
        return ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.EVALUATION_DISTRIBUTION)).getMean();
    }

    public long getTimeMillis() {
        return this.timeMillis;
    }
}
