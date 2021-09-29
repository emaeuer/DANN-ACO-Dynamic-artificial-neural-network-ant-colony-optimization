package de.emaeuer.cli;

import de.emaeuer.cli.parameter.AlgorithmCliParameter;
import de.emaeuer.cli.parameter.CliParameter;
import de.emaeuer.cli.parameter.NeatCliParameter;
import de.emaeuer.cli.parameter.DannacoCliParameter;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.evaluation.EvaluationConfiguration;
import de.emaeuer.optimization.OptimizationMethodNames;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.neat.configuration.NeatConfiguration;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.state.DannacoState;
import de.emaeuer.persistence.ConfigurationIOHandler;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.DataQuantityStateValue;
import de.emaeuer.state.value.DistributionStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CliLauncher {

    static {
        System.setProperty("logFilename", "variation" + System.currentTimeMillis());
        Locale.setDefault(Locale.US);
        LOG = LogManager.getLogger(CliLauncher.class);
    }

    private static final Logger LOG;

    private final CliParameter parameter;
    private final AlgorithmCliParameter algorithmParameters;

    private int maxEvaluations = 0;

    private boolean stoppedBecauseOfException = false;

    private final List<Double> time = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> evaluations = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> neurons = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> connections = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> success = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> deviation = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Long>> modifications = Collections.synchronizedList(new ArrayList<>());

    // rng for creating seeds for the runs
    private final Random rng = new Random();

    public static void main(String[] args) {
        CliLauncher launcher = CliLauncher.startFromArgs(args);
        launcher.run();
        System.out.println((launcher.getCost()) + " [" + launcher.getTimeMillis() + "]");
    }

    public CliLauncher(CliParameter generalParameters, AlgorithmCliParameter algorithmParameters) {
        this.parameter = generalParameters;
        this.algorithmParameters = algorithmParameters;

        this.rng.setSeed(this.parameter.getSeed());
    }

    public static CliLauncher startFromArgs(String[] args) {
        CliParameter parameters = new CliParameter();
        CommandLine.ParseResult result = new CommandLine(parameters).parseArgs(args);
        AlgorithmCliParameter algorithmParameter = (AlgorithmCliParameter) result.subcommand().commandSpec().userObject();

        return new CliLauncher(parameters, algorithmParameter);
    }

    private void parseErrorMessage(AlgorithmCliParameter algorithmParameters, String expectedConfiguration) {
        String cliConfiguration;

        if (algorithmParameters instanceof DannacoCliParameter) {
            cliConfiguration = "DANN_ACO";
        } else if (algorithmParameters instanceof NeatCliParameter) {
            cliConfiguration = "NEAT";
        } else {
            cliConfiguration = "UNKNOWN";
        }

        String message = String.format("The configuration file contains a configuration for %s but the cli parameters specify %s parameters", expectedConfiguration, cliConfiguration);
        LOG.warn(message);
        throw new IllegalArgumentException(message);
    }

    public void run() {
        if (this.parameter.getParallel() <= 1) {
            runSequential();
        } else {
            runParallel();
        }
    }

    private void runSequential() {
        for (int i = 0; i < this.parameter.getNumberOfRuns(); i++) {
            CliSingleRunData run = new CliSingleRunData();
            initRunData(run);

            long optimizationStart = System.nanoTime();
            run.getOptimization().run();
            updateResults(run, optimizationStart);
        }
    }

    private void runParallel() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.parameter.getNumberOfRuns());
        executor.setMaximumPoolSize(Math.max(this.parameter.getNumberOfRuns(), this.parameter.getParallel()));

        for (int i = 0; i < this.parameter.getNumberOfRuns() && !this.stoppedBecauseOfException; i++) {
            executor.execute(() -> {
                CliSingleRunData run = new CliSingleRunData();
                initRunData(run);

                long optimizationStart = System.nanoTime();
                run.getOptimization().run();
                updateResults(run, optimizationStart);
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(600, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.warn("Unexpected exception while waiting for the termination of all runs", e);
        }
    }

    private void initRunData(CliSingleRunData run) {
        ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(run.getConfiguration(), OptimizationConfiguration.class, EvaluationConfiguration.OPTIMIZATION_CONFIGURATION);
        ConfigurationHandler<EnvironmentConfiguration> environmentConfig = ConfigurationHelper.extractEmbeddedConfiguration(run.getConfiguration(), EnvironmentConfiguration.class, EvaluationConfiguration.ENVIRONMENT_CONFIGURATION);
        optimizationConfig.setName("OPTIMIZATION_CONFIGURATION");
        environmentConfig.setName("ENVIRONMENT_CONFIGURATION");
        run.getConfiguration().setName("CONFIGURATION");

        if (this.parameter.getConfigFile() != null && this.parameter.getConfigFile().exists()) {
            ConfigurationIOHandler.importConfiguration(this.parameter.getConfigFile(), run.getConfiguration());
        }

        if (parameter.getNumberOfRuns() == 1) {
            run.getConfiguration().setValue(EvaluationConfiguration.SEED, parameter.getSeed());
        } else {
            run.getConfiguration().setValue(EvaluationConfiguration.SEED, this.rng.nextInt());
        }

        run.getConfiguration().setValue(EvaluationConfiguration.MAX_TIME, this.parameter.getMaxTime());

        if (this.maxEvaluations == 0) {
            this.maxEvaluations = optimizationConfig.getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class);
        }

        if (algorithmParameters == null) {
            return;
        }

        if (algorithmParameters instanceof DannacoCliParameter dannacoParameter && OptimizationMethodNames.DANN_ACO.name().equals(optimizationConfig.getValue(OptimizationConfiguration.METHOD_NAME, String.class))) {
            ConfigurationHandler<DannacoConfiguration> config = ConfigurationHelper.extractEmbeddedConfiguration(optimizationConfig, DannacoConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
            this.applyParametersToDannacoConfig(config, dannacoParameter);
        } else if (algorithmParameters instanceof NeatCliParameter neatParameter && OptimizationMethodNames.NEAT.name().equals(optimizationConfig.getValue(OptimizationConfiguration.METHOD_NAME, String.class))) {
            ConfigurationHandler<NeatConfiguration> config = ConfigurationHelper.extractEmbeddedConfiguration(optimizationConfig, NeatConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);
            applyParametersToNeatConfig(config, neatParameter);
        } else {
            parseErrorMessage(algorithmParameters, optimizationConfig.getValue(OptimizationConfiguration.METHOD_NAME, String.class));
        }

        run.initialize();
    }

    private void applyParametersToDannacoConfig(ConfigurationHandler<DannacoConfiguration> config, DannacoCliParameter parameters) {
        Optional.ofNullable(parameters.getPopulationSize()).ifPresent(v -> config.setValue(DannacoConfiguration.POPULATION_SIZE, v));
        Optional.ofNullable(parameters.getUpdatesPerIteration()).ifPresent(v -> config.setValue(DannacoConfiguration.UPDATES_PER_ITERATION, v));
        Optional.ofNullable(parameters.getAntsPerIteration()).ifPresent(v -> config.setValue(DannacoConfiguration.ANTS_PER_ITERATION, v));
        Optional.ofNullable(parameters.getStandardDeviationFunction()).ifPresent(v -> config.setValue(DannacoConfiguration.DEVIATION_FUNCTION, v));
        Optional.ofNullable(parameters.getUpdateStrategy()).ifPresent(v -> config.setValue(DannacoConfiguration.UPDATE_STRATEGY, v));
        Optional.ofNullable(parameters.getTopologyPheromoneFunction()).ifPresent(v -> config.setValue(DannacoConfiguration.TOPOLOGY_PHEROMONE, v));
        Optional.ofNullable(parameters.getConnectionPheromoneFunction()).ifPresent(v -> config.setValue(DannacoConfiguration.CONNECTION_PHEROMONE, v));
        Optional.ofNullable(parameters.getSplitProbabilityFunction()).ifPresent(v -> config.setValue(DannacoConfiguration.SPLIT_PROBABILITY, v));
        Optional.ofNullable(parameters.isElitism()).ifPresent(v -> config.setValue(DannacoConfiguration.ELITISM, v));
        Optional.ofNullable(parameters.isNeuronIsolation()).ifPresent(v -> config.setValue(DannacoConfiguration.ENABLE_NEURON_ISOLATION, v));
        Optional.ofNullable(parameters.getSolutionWeightFactor()).ifPresent(v -> config.setValue(DannacoConfiguration.SOLUTION_WEIGHT_FACTOR, v));
        Optional.ofNullable(parameters.isReuseSplitKnowledge()).ifPresent(v -> config.setValue(DannacoConfiguration.REUSE_SPLIT_KNOWLEDGE, v));
        Optional.ofNullable(parameters.getUpsilon()).ifPresent(v -> config.setValue(DannacoConfiguration.TOPOLOGY_SIMILARITY_THRESHOLD, v));
    }

    private void applyParametersToNeatConfig(ConfigurationHandler<NeatConfiguration> config, NeatCliParameter parameters) {
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
    }

    private void updateResults(CliSingleRunData run, long startTimeNanos) {
        if (this.algorithmParameters instanceof DannacoCliParameter) {
            //noinspection unchecked
            StateHandler<DannacoState> state = run.getOptimizationState().getValue(OptimizationState.IMPLEMENTATION_STATE, StateHandler.class);
            Map<String, Long> modificationQuantities = ((DataQuantityStateValue) state.getCurrentState().get(DannacoState.MODIFICATION_DISTRIBUTION)).getValue();
            this.modifications.add(modificationQuantities);

            this.deviation.addAll(((DistributionStateValue) state.getCurrentState().get(DannacoState.AVERAGE_STANDARD_DEVIATION)).getValue());
        }

        this.stoppedBecauseOfException |= run.getOptimization().stoppedBecauseOfException();
        this.time.add((System.nanoTime() - startTimeNanos) / 1000000.0);
        this.evaluations.addAll(((DistributionStateValue) run.getOptimizationState().getCurrentState().get(OptimizationState.EVALUATION_DISTRIBUTION)).getValue());
        this.neurons.addAll(((DistributionStateValue) run.getOptimizationState().getCurrentState().get(OptimizationState.HIDDEN_NODES_DISTRIBUTION)).getValue());
        this.connections.addAll(((DistributionStateValue) run.getOptimizationState().getCurrentState().get(OptimizationState.CONNECTIONS_DISTRIBUTION)).getValue());
        this.success.addAll(((DistributionStateValue) run.getOptimizationState().getCurrentState().get(OptimizationState.FINISHED_RUN_DISTRIBUTION)).getValue());

    }

    public double getCost() {
        if (this.stoppedBecauseOfException) {
            return Double.POSITIVE_INFINITY;
        }

        double runsNotFinished = 1 - getSuccessRate();
        return getEvaluations() + runsNotFinished * this.maxEvaluations;
    }

    public double getTimeMillis() {
        return this.time.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(-1);
    }

    public double getNumberOfHiddenNodes() {
        return this.neurons.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(-1);
    }

    public double getNumberOfConnections() {
        return this.connections.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(-1);
    }

    public double getSuccessRate() {
        return this.success.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(-1);
    }

    public String getAllEvaluations() {
        return this.evaluations.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public double getEvaluations() {
        return this.evaluations.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(-1);
    }

    public double getDeviations() {
        return this.deviation.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(-1);
    }

    public String getModificationString() {
        Map<String, Long> cumulatedData = this.modifications.stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));

        long sum = cumulatedData.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return cumulatedData.entrySet()
                .stream()
                .map(e -> String.format("%s:%d (%.2f)", e.getKey(), e.getValue(), e.getValue().doubleValue() / sum))
                .collect(Collectors.joining(", "));

    }
}
