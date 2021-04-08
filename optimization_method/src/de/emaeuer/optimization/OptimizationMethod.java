package de.emaeuer.optimization;

import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.util.GraphHelper;
import de.emaeuer.optimization.util.ProgressionHandler;
import de.emaeuer.optimization.util.RunDataHandler;
import de.emaeuer.optimization.util.RunDataHandler.RunSummary;
import de.emaeuer.persistence.SingletonDataExporter;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

public abstract class OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(OptimizationMethod.class);

    protected static final String TOTAL_MAX = "Best fitness";
    protected static final String MAX = "Maximum fitness";
    protected static final String MIN = "Minimum fitness";
    protected static final String AVERAGE = "Average fitness";

    private int runCounter = 0;
    private int generationCounter = 0;
    private int evaluationCounter = 0;
    private double bestFitness = 0;

    private final ConfigurationHandler<OptimizationConfiguration> configuration;
    private final StateHandler<OptimizationState> generalState;

    private Solution currentlyBestSolution;

    private ProgressionHandler progressionHandler;
    private final RunDataHandler averageHandler;

    private boolean optimizationFinished = false;
    private boolean runFinished = false;

    protected OptimizationMethod(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        this.configuration = configuration;
        this.generalState = generalState;

        this.averageHandler = new RunDataHandler(this.generalState);

        logConfiguration(configuration);

        incrementRunCounter();

        this.progressionHandler = new ProgressionHandler(
                configuration.getValue(OptimizationConfiguration.PROGRESSION_ITERATIONS, Integer.class),
                configuration.getValue(OptimizationConfiguration.PROGRESSION_THRESHOLD, Double.class));
    }

    private void logConfiguration(ConfigurationHandler<OptimizationConfiguration> handler) {
        LOG.info("Created optimization method with the following configuration:");

        int maxKeyLength = handler.getConfigurations()
                .keySet()
                .stream()
                .map(k -> k.toString().length())
                .max(Integer::compareTo)
                .orElse(0);

        handler.getConfigurations()
                .entrySet()
                .stream()
                .map(e -> String.format("%-" + maxKeyLength + "s = %s", e.getKey(), e.getValue()))
                .forEach(LOG::info);
    }

    public List<? extends Solution> nextIteration() {
        // do nothing if the optimization finished (maximum fitness or max number of evaluations reached)
        if (this.optimizationFinished) {
            return Collections.emptyList();
        }

        incrementGenerationCounter();

        List<? extends Solution> solutions = generateSolutions();

        this.evaluationCounter += solutions.size();

        return solutions;
    }

    public void update() {
        // do nothing if the optimization finished (maximum fitness or max number of evaluations reached)
        if (this.optimizationFinished || this.evaluationCounter == 0) {
            return;
        }

        updateFitnessScore();

        this.progressionHandler.addFitnessScore(this.bestFitness);
        if (this.progressionHandler.doesStagnate()) {
            handleProgressionStagnation();
        }

        if (checkCurrentRunFinished() && checkOptimizationFinished()) {
            // optimization finished completely
            this.runFinished = true;
            this.optimizationFinished = true;

            // final update of optimization state
            updateRunStatistics();
        } else if (checkCurrentRunFinished()) {
            // only the current run finished restart new optimization
            this.runFinished = true;
            updateRunStatistics();
        }
    }

    public void resetAndRestart() {
        this.runFinished = false;

        this.generalState.resetValue(OptimizationState.CURRENT_ITERATION);
        this.generalState.resetValue(OptimizationState.FITNESS_VALUE);
        this.generalState.resetValue(OptimizationState.FITNESS_SERIES);
        this.generalState.resetValue(OptimizationState.IMPLEMENTATION_STATE);
        this.generalState.resetValue(OptimizationState.BEST_SOLUTION);

        this.currentlyBestSolution = null;

        incrementRunCounter();
        this.generationCounter = 0;
        this.evaluationCounter = 0;
        this.bestFitness = 0;

        this.progressionHandler = new ProgressionHandler(
                configuration.getValue(OptimizationConfiguration.PROGRESSION_ITERATIONS, Integer.class),
                configuration.getValue(OptimizationConfiguration.PROGRESSION_THRESHOLD, Double.class));
    }

    protected void updateRunStatistics() {
        int numberOfHiddenNodes = -1;
        int numberOfConnections = -1;

        if (this.currentlyBestSolution != null && this.currentlyBestSolution.getNeuralNetwork() != null) {
            numberOfHiddenNodes = NeuralNetworkUtil.countHiddenNodes(this.currentlyBestSolution.getNeuralNetwork());
            numberOfConnections = NeuralNetworkUtil.countConnections(this.currentlyBestSolution.getNeuralNetwork());
        }

        RunSummary summary = new RunSummary(this.bestFitness, this.evaluationCounter, numberOfHiddenNodes, numberOfConnections);
        this.averageHandler.addSummaryOfRun(summary);
    }

    private void incrementGenerationCounter() {
        this.generationCounter++;
        this.generalState.addNewValue(OptimizationState.CURRENT_ITERATION, this.generationCounter);

        LOG.info("Generating solutions for iteration {}", this.generationCounter);
    }

    protected void incrementRunCounter() {
        this.runCounter++;
        this.generalState.addNewValue(OptimizationState.CURRENT_RUN, this.runCounter);

        LOG.info("Starting run {}", this.runCounter);
    }

    protected boolean checkOptimizationFinished() {
        // finished if: maximal run number reached and the current run finished
        return checkCurrentRunFinished() && this.runCounter >= this.configuration.getValue(OptimizationConfiguration.NUMBER_OF_RUNS, Integer.class);
    }

    public boolean checkCurrentRunFinished() {
        // finished if: maximal fitness or evaluation reached
        return this.bestFitness >= this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class) ||
                this.evaluationCounter >= this.configuration.getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class);
    }

    protected void handleProgressionStagnation() {
        this.progressionHandler.resetProgression();
    }

    protected void updateFitnessScore() {
        exportFitnessValues();

        DoubleSummaryStatistics currentFitness = getFitnessOfIteration();
        this.bestFitness = Double.max(this.bestFitness, currentFitness.getMax());

        this.generalState.addNewValue(OptimizationState.FITNESS_VALUE, this.bestFitness);
        this.generalState.addNewValue(OptimizationState.FITNESS_SERIES, new SimpleEntry<>(TOTAL_MAX, new Double[] {(double) getEvaluationCounter(), this.bestFitness}));
        this.generalState.addNewValue(OptimizationState.FITNESS_SERIES, new SimpleEntry<>(MAX, new Double[] {(double) getEvaluationCounter(), currentFitness.getMax()}));
        this.generalState.addNewValue(OptimizationState.FITNESS_SERIES, new SimpleEntry<>(MIN, new Double[] {(double) getEvaluationCounter(), currentFitness.getMin()}));
        this.generalState.addNewValue(OptimizationState.FITNESS_SERIES, new SimpleEntry<>(AVERAGE, new Double[] {(double) getEvaluationCounter(), currentFitness.getAverage()}));

        this.averageHandler.addFitnessSummary(getEvaluationCounter(), currentFitness);
    }

    protected void exportFitnessValues() {
        List<Double> fitnessValues = getCurrentSolutions()
                .stream()
                .map(Solution::getFitness)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("evaluation", getEvaluationCounter());
        data.put("values", fitnessValues);

        SingletonDataExporter.addRunData("fitness", data, true);
    }

    protected void updateBestSolution() {
        this.generalState.addNewValue(OptimizationState.BEST_SOLUTION, GraphHelper.transformToConnectionList(this.currentlyBestSolution.getNeuralNetwork()));
    }

    protected abstract List<? extends Solution> getCurrentSolutions();

    protected abstract DoubleSummaryStatistics getFitnessOfIteration();

    protected abstract List<? extends Solution> generateSolutions();

    public int getEvaluationCounter() {
        return this.evaluationCounter;
    }

    public Solution getCurrentlyBestSolution() {
        return currentlyBestSolution;
    }

    protected void setCurrentlyBestSolution(Solution currentBest) {
        if (currentBest != null && currentBest.getFitness() > this.bestFitness) {
            this.currentlyBestSolution = currentBest;
            updateBestSolution();
        }
    }

    public boolean isOptimizationFinished() {
        return optimizationFinished;
    }

    public boolean isRunFinished() {
        return runFinished;
    }

    public double getBestFitness() {
        return bestFitness;
    }

    public double getFitnessThreshold() {
        return this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);
    }

    public int getEvaluationThreshold() {
        return this.configuration.getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class);
    }

    public int getRunCounter() {
        return this.runCounter;
    }

    public int getMaxNumberOfRuns() {
        return this.configuration.getValue(OptimizationConfiguration.NUMBER_OF_RUNS, Integer.class);
    }

    public int getGenerationCounter() {
        return generationCounter;
    }

    public ConfigurationHandler<OptimizationConfiguration> getOptimizationConfiguration() {
        return this.configuration;
    }

    public StateHandler<OptimizationState> getState() {
        return this.generalState;
    }

}
