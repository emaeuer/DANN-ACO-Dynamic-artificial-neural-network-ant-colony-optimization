package de.emaeuer.cli;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.evaluation.OptimizationEnvironmentHandler;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.persistence.ConfigurationIOHandler;
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

    private final OptimizationEnvironmentHandler optimization = new OptimizationEnvironmentHandler();

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

        AlternativeCliParameter parameters = new AlternativeCliParameter();
        new CommandLine(parameters).parseArgs(args);

        initConfigurations(parameters.getConfigFile(), parameters);
    }

    public CliLauncher(String[] args, int seed) {
        LOG.debug("CLI-Call-Parameters: " + Arrays.toString(args));

        AlternativeCliParameter parameters = new AlternativeCliParameter();
        new CommandLine(parameters).parseArgs(args);

        initConfigurations(parameters.getConfigFile(), parameters);

        // seed of the environment should not be changed to have the same difficulty
//        this.environmentConfiguration.setValue(EnvironmentConfiguration.SEED, seed);
        this.optimizationConfiguration.setValue(OptimizationConfiguration.SEED, seed);
    }

    private void initConfigurations(File file, AlternativeCliParameter parameters) {
        this.environmentConfiguration.setName("ENVIRONMENT_CONFIGURATION");
        this.optimizationConfiguration.setName("OPTIMIZATION_CONFIGURATION");

        if (file != null && file.exists()) {
            ConfigurationIOHandler.importConfiguration(file, this.environmentConfiguration, this.optimizationConfiguration);
        }

        if (OptimizationMethodNames.PACO.name().equals(optimizationConfiguration.getValue(OptimizationConfiguration.METHOD_NAME, String.class))) {
            ConfigurationHandler<PacoConfiguration> config = ConfigurationHelper.extractEmbeddedConfiguration(this.optimizationConfiguration, PacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
            applyParametersToConfig(config, parameters);
        }
    }

    private void applyParametersToConfig(ConfigurationHandler<PacoConfiguration> config, AlternativeCliParameter parameters) {
        Optional.ofNullable(parameters.getPopulationSize()).ifPresent(v -> config.setValue(PacoConfiguration.POPULATION_SIZE, v));
        Optional.ofNullable(parameters.getUpdatesPerIteration()).ifPresent(v -> config.setValue(PacoConfiguration.UPDATES_PER_ITERATION, v));
        Optional.ofNullable(parameters.getAntsPerIteration()).ifPresent(v -> config.setValue(PacoConfiguration.ANTS_PER_ITERATION, v));
        Optional.ofNullable(parameters.getStandardDeviationFunction()).ifPresent(v -> config.setValue(PacoConfiguration.DEVIATION_FUNCTION, v));
        Optional.ofNullable(parameters.getUpdateStrategy()).ifPresent(v -> config.setValue(PacoConfiguration.UPDATE_STRATEGY, v));
        Optional.ofNullable(parameters.getTopologyPheromoneFunction()).ifPresent(v -> config.setValue(PacoConfiguration.TOPOLOGY_PHEROMONE, v));
        Optional.ofNullable(parameters.getConnectionPheromoneFunction()).ifPresent(v -> config.setValue(PacoConfiguration.CONNECTION_PHEROMONE, v));
        Optional.ofNullable(parameters.getSplitProbabilityFunction()).ifPresent(v -> config.setValue(PacoConfiguration.SPLIT_PROBABILITY, v));
        Optional.ofNullable(parameters.isElitism()).ifPresent(v -> config.setValue(PacoConfiguration.ELITISM, v));
        Optional.ofNullable(parameters.isNeuronIsolation()).ifPresent(v -> config.setValue(PacoConfiguration.ENABLE_NEURON_ISOLATION, v));
        Optional.ofNullable(parameters.getSolutionWeightFactor()).ifPresent(v -> config.setValue(PacoConfiguration.SOLUTION_WEIGHT_FACTOR, v));
        Optional.ofNullable(parameters.isReuseSplitKnowledge()).ifPresent(v -> config.setValue(PacoConfiguration.REUSE_SPLIT_KNOWLEDGE, v));
    }

    public void run() {
        this.optimization.setOptimizationState(this.optimizationState);
        this.optimization.setOptimizationConfiguration(this.optimizationConfiguration);
        this.optimization.setEnvironmentConfiguration(this.environmentConfiguration);
        this.optimization.setUpdateDelta(0);

        this.optimization.initialize();

        long startTime = System.nanoTime();
        this.optimization.run();
        this.timeMillis = (System.nanoTime() - startTime) / 1000000;
    }

    public double getCost() {
        double runsNotFinished = 1 - ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.FINISHED_RUN_DISTRIBUTION)).getMean();
        return ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.EVALUATION_DISTRIBUTION)).getMean() + runsNotFinished * this.optimization.getMaxEvaluations();
    }

    public long getTimeMillis() {
        return this.timeMillis;
    }
}
