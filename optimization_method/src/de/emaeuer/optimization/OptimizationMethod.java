package de.emaeuer.optimization;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.util.GraphHelper;
import de.emaeuer.optimization.util.ProgressionHandler;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;

public abstract class OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(OptimizationMethod.class);

    protected static final String TOTAL_MAX = "Best fitness";
    protected static final String MAX = "Maximum fitness";
    protected static final String MIN = "Minimum fitness";
    protected static final String AVERAGE = "Average fitness";

    private int generationCounter = 0;
    private int evaluationCounter = 0;
    private double maxFitness = 0;

    private final ConfigurationHandler<OptimizationConfiguration> configuration;
    private final StateHandler<OptimizationState> generalState;

    private Solution currentlyBestSolution;

    private final ProgressionHandler progressionHandler;

    private boolean optimizationFinished = false;

    protected OptimizationMethod(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        this.configuration = configuration;
        this.generalState = generalState;

        logConfiguration(configuration);

        this.progressionHandler = new ProgressionHandler(
                configuration.getValue(OptimizationConfiguration.OPTIMIZATION_PROGRESSION_ITERATIONS, Integer.class),
                configuration.getValue(OptimizationConfiguration.OPTIMIZATION_PROGRESSION_THRESHOLD, Double.class));
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

        this.generalState.addNewValue(OptimizationState.ITERATION, this.generationCounter);
        this.generationCounter++;

        LOG.info("Generating solutions for iteration {}", generationCounter);

        List<? extends Solution> solutions = generateSolutions();

        this.evaluationCounter += solutions.size();

        return solutions;
    }

    public void update() {
        // do nothing if the optimization finished (maximum fitness or max number of evaluations reached)
        if (this.optimizationFinished) {
            return;
        }

        updateFitnessScore();

        this.progressionHandler.addFitnessScore(this.maxFitness);
        if (this.progressionHandler.doesStagnate()) {
            handleProgressionStagnation();
        }

        checkOptimizationFinished();
    }

    protected void checkOptimizationFinished() {
        this.optimizationFinished |= this.maxFitness >= this.configuration.getValue(OptimizationConfiguration.OPTIMIZATION_MAX_FITNESS_SCORE, Double.class);
        this.optimizationFinished |= this.evaluationCounter >= this.configuration.getValue(OptimizationConfiguration.OPTIMIZATION_MAX_NUMBER_OF_EVALUATIONS, Integer.class);
    }

    protected void handleProgressionStagnation() {
        this.progressionHandler.resetProgression();
    }

    protected void updateFitnessScore() {
        DoubleSummaryStatistics currentFitness = getFitnessOfIteration();
        this.maxFitness = Double.max(this.maxFitness, currentFitness.getMax());

        this.generalState.addNewValue(OptimizationState.FITNESS, new SimpleEntry<>(TOTAL_MAX, new Double[] {(double) getEvaluationCounter(), this.maxFitness}));
        this.generalState.addNewValue(OptimizationState.FITNESS, new SimpleEntry<>(MAX, new Double[] {(double) getEvaluationCounter(), currentFitness.getMax()}));
        this.generalState.addNewValue(OptimizationState.FITNESS, new SimpleEntry<>(MIN, new Double[] {(double) getEvaluationCounter(), currentFitness.getMin()}));
        this.generalState.addNewValue(OptimizationState.FITNESS, new SimpleEntry<>(AVERAGE, new Double[] {(double) getEvaluationCounter(), currentFitness.getAverage()}));
    }

    protected void updateBestSolution() {
        this.generalState.addNewValue(OptimizationState.BEST_SOLUTION, GraphHelper.transformToConnectionList(this.currentlyBestSolution.getNeuralNetwork()));
    }

    protected abstract DoubleSummaryStatistics getFitnessOfIteration();

    protected abstract List<? extends Solution> generateSolutions();

    public int getEvaluationCounter() {
        return this.evaluationCounter;
    }

    public Solution getCurrentlyBestSolution() {
        return currentlyBestSolution;
    }

    protected void setCurrentlyBestSolution(Solution currentBest) {
        if (currentBest != null && currentBest.getFitness() > this.maxFitness) {
            this.currentlyBestSolution = currentBest;
            updateBestSolution();
        }
    }

    public boolean isOptimizationFinished() {
        return optimizationFinished;
    }

    public double getMaxFitness() {
        return maxFitness;
    }

    public double getFitnessThreshold() {
        return this.configuration.getValue(OptimizationConfiguration.OPTIMIZATION_MAX_FITNESS_SCORE, Double.class);
    }

    public int getEvaluationThreshold() {
        return this.configuration.getValue(OptimizationConfiguration.OPTIMIZATION_MAX_NUMBER_OF_EVALUATIONS, Integer.class);
    }
}
