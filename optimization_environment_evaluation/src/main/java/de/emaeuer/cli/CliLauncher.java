package de.emaeuer.cli;

import de.emaeuer.cli.parameter.CliParameter;
import de.emaeuer.cli.parameter.NeatCliParameter;
import de.emaeuer.cli.parameter.DannacoCliParameter;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.evaluation.EvaluationConfiguration;
import de.emaeuer.evaluation.OptimizationEnvironmentHandler;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationRunState;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.neat.configuration.NeatConfiguration;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.persistence.ConfigurationIOHandler;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.DistributionStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class CliLauncher {

    private static final Logger LOG = LogManager.getLogger(CliLauncher.class);

    private final OptimizationEnvironmentHandler optimization = new OptimizationEnvironmentHandler();

    private final StateHandler<OptimizationState> optimizationState = new StateHandler<>(OptimizationState.class);
    private final ConfigurationHandler<EvaluationConfiguration> configuration = new ConfigurationHandler<>(EvaluationConfiguration.class);

    private long timeMillis = -1;

    public static void main(String[] args) {
        CliLauncher launcher = new CliLauncher(args);
        launcher.run();
        System.out.println((-1 * launcher.getCost()) + " [" + launcher.getTimeMillis() + "]");
    }

    public CliLauncher(String[] args) {
        LOG.debug("CLI-Call-Parameters: " + Arrays.toString(args));

        CliParameter parameters = new CliParameter();
        CommandLine.ParseResult result = new CommandLine(parameters).parseArgs(args);

        if (result.hasSubcommand()) {
            initConfigurations(parameters, result.subcommand().commandSpec().userObject());
        } else {
            initConfigurations(parameters, null);
        }
    }

    public CliLauncher(String[] args, int seed) {
        LOG.debug("CLI-Call-Parameters: " + Arrays.toString(args));

        CliParameter parameters = new CliParameter();
        CommandLine.ParseResult result = new CommandLine(parameters).parseArgs(args);

        if (result.hasSubcommand()) {
            initConfigurations(parameters, result.subcommand().commandSpec().userObject());
        } else {
            initConfigurations(parameters, null);
        }

        this.configuration.setValue(EvaluationConfiguration.SEED, seed);
    }

    private void initConfigurations(CliParameter basicParameters, Object parameters) {
        ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(this.configuration, OptimizationConfiguration.class, EvaluationConfiguration.OPTIMIZATION_CONFIGURATION);
        ConfigurationHandler<EnvironmentConfiguration> environmentConfig = ConfigurationHelper.extractEmbeddedConfiguration(this.configuration, EnvironmentConfiguration.class, EvaluationConfiguration.ENVIRONMENT_CONFIGURATION);
        optimizationConfig.setName("OPTIMIZATION_CONFIGURATION");
        environmentConfig.setName("ENVIRONMENT_CONFIGURATION");
        this.configuration.setName("CONFIGURATION");

        if (basicParameters.getConfigFile() != null && basicParameters.getConfigFile().exists()) {
            ConfigurationIOHandler.importConfiguration(basicParameters.getConfigFile(), this.configuration);
        }

        this.configuration.setValue(EvaluationConfiguration.MAX_TIME, basicParameters.getMaxTime());

        if (parameters == null) {
            return;
        }

        try {
            if (OptimizationMethodNames.PACO.name().equals(optimizationConfig.getValue(OptimizationConfiguration.METHOD_NAME, String.class))) {
                ConfigurationHandler<PacoConfiguration> config = ConfigurationHelper.extractEmbeddedConfiguration(optimizationConfig, PacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
                this.applyParametersToPacoConfig(config, parameters);
            } else if (OptimizationMethodNames.NEAT.name().equals(optimizationConfig.getValue(OptimizationConfiguration.METHOD_NAME, String.class))) {
                ConfigurationHandler<NeatConfiguration> config = ConfigurationHelper.extractEmbeddedConfiguration(optimizationConfig, NeatConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
                applyParametersToNeatConfig(config, parameters);
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Configuration in file and arguments doesn't match", e);
            throw e;
        }
    }

    private void applyParametersToPacoConfig(ConfigurationHandler<PacoConfiguration> config, Object parameterObj) {
        if (parameterObj instanceof DannacoCliParameter parameters) {
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
            Optional.ofNullable(parameters.getUpsilon()).ifPresent(v -> config.setValue(PacoConfiguration.TOPOLOGY_SIMILARITY_THRESHOLD, v));

            Optional.ofNullable(parameters.getModificationRate()).ifPresent(v -> config.setValue(PacoConfiguration.MODIFICATION_RATE, v));
            Optional.ofNullable(parameters.getAddConnectionRate()).ifPresent(v -> config.setValue(PacoConfiguration.ADD_CONNECTION_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getRemoveConnectionRate()).ifPresent(v -> config.setValue(PacoConfiguration.REMOVE_CONNECTION_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getAddNeuronRate()).ifPresent(v -> config.setValue(PacoConfiguration.ADD_NEURON_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getStaticStandardDeviation()).ifPresent(v -> config.setValue(PacoConfiguration.STATIC_STANDARD_DEVIATION, v));
            Optional.ofNullable(parameters.getSplitRate()).ifPresent(v -> config.setValue(PacoConfiguration.SPLIT_RATE, v));
        } else {
            LOG.warn("Failed to apply parameters of type {} to PacoConfiguration", parameterObj.getClass().getSimpleName());
        }
    }

    private void applyParametersToNeatConfig(ConfigurationHandler<NeatConfiguration> config, Object parameterObj) {
        if (parameterObj instanceof NeatCliParameter parameters) {
            Optional.ofNullable(parameters.getSurvivalRate()).ifPresent(v -> config.setValue(NeatConfiguration.SURVIVAL_RATE, v));
            Optional.ofNullable(parameters.getTopologyMutationType()).ifPresent(v -> config.setValue(NeatConfiguration.TOPOLOGY_MUTATION_CLASSIC, v));
            Optional.ofNullable(parameters.getPopulationSize()).ifPresent(v -> config.setValue(NeatConfiguration.POPULATION_SIZE, v));
            Optional.ofNullable(parameters.getChromosomeCompatibilityExcessCoefficient()).ifPresent(v -> config.setValue(NeatConfiguration.CHROM_COMPAT_EXCESS_COEFF, v));
            Optional.ofNullable(parameters.getChromosomeCompatibilityDisjointCoefficient()).ifPresent(v -> config.setValue(NeatConfiguration.CHROM_COMPAT_DISJOINT_COEFF, v));
            Optional.ofNullable(parameters.getChromosomeCompatibilityCommonCoefficient()).ifPresent(v -> config.setValue(NeatConfiguration.CHROM_COMPAT_COMMON_COEFF, v));
            Optional.ofNullable(parameters.getSpeciationThreshold()).ifPresent(v -> config.setValue(NeatConfiguration.SPECIATION_THRESHOLD, v));
            Optional.ofNullable(parameters.getUseElitism()).ifPresent(v -> config.setValue(NeatConfiguration.ELITISM, v));
            Optional.ofNullable(parameters.getElitismMinSpeciesSize()).ifPresent(v -> config.setValue(NeatConfiguration.ELITISM_MIN_SPECIE_SIZE, v));
            Optional.ofNullable(parameters.getUseRouletteSelection()).ifPresent(v -> config.setValue(NeatConfiguration.WEIGHTED_SELECTOR, v));
            Optional.ofNullable(parameters.getAddConnectionMutationRate()).ifPresent(v -> config.setValue(NeatConfiguration.ADD_CONNECTION_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getAddNeuronMutationRate()).ifPresent(v -> config.setValue(NeatConfiguration.ADD_NEURON_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getRemoveConnectionMutationRate()).ifPresent(v -> config.setValue(NeatConfiguration.REMOVE_CONNECTION_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getRemoveConnectionMaxWeight()).ifPresent(v -> config.setValue(NeatConfiguration.REMOVE_CONNECTION_MAX_WEIGHT, v));
            Optional.ofNullable(parameters.getRemoveConnectionStrategy()).ifPresent(v -> config.setValue(NeatConfiguration.REMOVE_CONNECTION_STRATEGY, v));
            Optional.ofNullable(parameters.getPruneMutationRate()).ifPresent(v -> config.setValue(NeatConfiguration.PRUNE_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getWeightMutationRate()).ifPresent(v -> config.setValue(NeatConfiguration.WEIGHT_MUTATION_RATE, v));
            Optional.ofNullable(parameters.getWeightMutationDeviation()).ifPresent(v -> config.setValue(NeatConfiguration.WEIGHT_MUTATION_DEVIATION, v));

            // use nanos for file name to have unique file names
            long nanos = System.nanoTime();
            config.setValue(NeatConfiguration.ID_FILE, String.format("neat/id_%d.xml", nanos));
            config.setValue(NeatConfiguration.NEAT_ID_FILE, String.format("neat/neat_id_%d.xml", nanos));
        } else {
            LOG.warn("Failed to apply parameters of type {} to NeatConfiguration", parameterObj.getClass().getSimpleName());
        }
    }

    public void run() {
        this.optimization.setOptimizationState(this.optimizationState);
        this.optimization.setConfiguration(this.configuration);
        this.optimization.setUpdateDelta(0);

        this.optimization.initialize();

        long startTime = System.nanoTime();
        this.optimization.run();
        this.timeMillis = (System.nanoTime() - startTime) / 1000000;
    }

    public double getCost() {
        if (this.optimization.stoppedBecauseOfException()) {
            return Double.POSITIVE_INFINITY;
        }

//        GraphStateValue.GraphData bestTopology = this.optimizationState.getValue(OptimizationState.GLOBAL_BEST_SOLUTION, GraphStateValue.GraphData.class);
//        long numberOfHiddenNodes = bestTopology.connections()
//                .stream()
//                .flatMap(c -> Stream.of(c.start(), c.target()))
//                .distinct()
//                .filter(n -> n.startsWith("1-"))
//                .count();
//
//        double runsNotFinished = 1 - ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.FINISHED_RUN_DISTRIBUTION)).getMean();
//        double evaluationCost = ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.EVALUATION_DISTRIBUTION)).getMean() / this.optimization.getMaxEvaluations();
//        evaluationCost = Math.min(1, evaluationCost);
//        double topologyCost = 1;
//
//        if (runsNotFinished == 0) {
//            topologyCost = 1 - (1 / (0.5 * numberOfHiddenNodes + 1));
//        }
//
//        return topologyCost * 0.5 + evaluationCost * 0.5;
        // penalty for runs that didn't finish
        double runsNotFinished = 1 - ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.FINISHED_RUN_DISTRIBUTION)).getMean();
        return ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.EVALUATION_DISTRIBUTION)).getMean() + runsNotFinished * this.optimization.getMaxEvaluations();
    }

    public long getTimeMillis() {
        return this.timeMillis;
    }

    public double getNumberOfHiddenNodes() {
        return ((DistributionStateValue) this.optimizationState.getCurrentState().get(OptimizationState.HIDDEN_NODES_DISTRIBUTION)).getMean();
    }

    public double getNumberOfConnections() {
        return ((DistributionStateValue) this.optimizationState.getCurrentState().get(OptimizationState.CONNECTIONS_DISTRIBUTION)).getMean();
    }

    public double getSuccessRate() {
        return ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.FINISHED_RUN_DISTRIBUTION)).getMean();
    }

    public StateHandler<OptimizationState> getOptimizationState() {
        return optimizationState;
    }

    public String getAllEvaluations() {
        return ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.EVALUATION_DISTRIBUTION))
                .getValue()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public double getEvaluations() {
        return ((DistributionStateValue ) this.optimizationState.getCurrentState().get(OptimizationState.EVALUATION_DISTRIBUTION))
                .getMean();
    }

}
