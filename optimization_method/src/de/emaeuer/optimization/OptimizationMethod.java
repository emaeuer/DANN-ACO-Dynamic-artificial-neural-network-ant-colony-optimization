package de.emaeuer.optimization;

import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationRunState;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.util.GraphHelper;
import de.emaeuer.optimization.util.ProgressionHandler;
import de.emaeuer.optimization.util.RunDataHandler;
import de.emaeuer.optimization.util.RunDataHandler.RunSummary;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.GraphStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

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
    private final StateHandler<OptimizationRunState> runState;

    private Solution currentlyBestSolution;
    private Solution overallBestSolution;

    private ProgressionHandler progressionHandler;
    private final RunDataHandler averageHandler;

    private boolean optimizationFinished = false;
    private boolean runFinished = false;

    protected OptimizationMethod(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        this.configuration = configuration;
        this.generalState = generalState;
        this.runState = new StateHandler<>(OptimizationRunState.class, generalState);
        this.generalState.execute(t -> t.addNewValue(OptimizationState.STATE_OF_CURRENT_RUN, this.runState));

        this.averageHandler = new RunDataHandler(this.generalState, this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class));

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
        this.runState.execute(t -> t.addNewValue(OptimizationRunState.EVALUATION_NUMBER, this.evaluationCounter));

        return solutions;
    }

    public void update() {
        // do nothing if the optimization finished (maximum fitness or max number of evaluations reached)
        if (this.optimizationFinished || this.evaluationCounter == 0) {
            return;
        }

        updateState();

        this.progressionHandler.addFitnessScore(this.bestFitness);
        if (this.progressionHandler.doesStagnate()) {
            handleProgressionStagnation();
        }

        if (checkCurrentRunFinished() && checkOptimizationFinished()) {
            // optimization finished completely
            this.runFinished = true;
            this.optimizationFinished = true;

            // final update of optimization state
            updateRunState();
            updateGeneralState();
            updateRunStatistics();
            exportSummary();
        } else if (checkCurrentRunFinished()) {
            // only the current run finished restart new optimization
            this.runFinished = true;
            updateGeneralState();
            updateRunState();
            updateRunStatistics();
        }
    }

    public void resetAndRestart() {
        incrementRunCounter();
        this.currentlyBestSolution = null;
        this.runFinished = false;
        this.generationCounter = 0;
        this.evaluationCounter = 0;
        this.bestFitness = 0;

        this.progressionHandler = new ProgressionHandler(
                configuration.getValue(OptimizationConfiguration.PROGRESSION_ITERATIONS, Integer.class),
                configuration.getValue(OptimizationConfiguration.PROGRESSION_THRESHOLD, Double.class));
    }

    private void updateGeneralState() {
        boolean finishedPremature = this.bestFitness >= this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);

        this.generalState.execute(t -> {
            t.addNewValue(OptimizationState.EVALUATION_DISTRIBUTION, Integer.valueOf(this.evaluationCounter).doubleValue());
            t.addNewValue(OptimizationState.FITNESS_DISTRIBUTION, this.bestFitness);
            t.addNewValue(OptimizationState.FINISHED_RUN_DISTRIBUTION, finishedPremature ? 1.0 : 0.0);
        });

        if (this.currentlyBestSolution != null && this.currentlyBestSolution.getNeuralNetwork() != null) {
            double numberOfHiddenNodes = NeuralNetworkUtil.countHiddenNodes(this.currentlyBestSolution.getNeuralNetwork());
            double numberOfConnections = NeuralNetworkUtil.countConnections(this.currentlyBestSolution.getNeuralNetwork());



            this.generalState.execute(t -> {
                t.addNewValue(OptimizationState.HIDDEN_NODES_DISTRIBUTION, numberOfHiddenNodes);
                t.addNewValue(OptimizationState.CONNECTIONS_DISTRIBUTION, numberOfConnections);
            });
        }
    }

    private void updateRunState() {
        boolean finishedPremature = this.bestFitness >= this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);

        this.runState.execute(t -> {
            t.export(OptimizationRunState.CURRENT_BEST_SOLUTION);
            t.addNewValue(OptimizationRunState.RUN_FINISHED, finishedPremature ? 1 : 0);
            t.resetValue(OptimizationRunState.FITNESS_VALUE);
            t.resetValue(OptimizationRunState.CURRENT_BEST_SOLUTION);
            t.resetValue(OptimizationRunState.RUN_FINISHED);
            t.resetValue(OptimizationRunState.RUN_NUMBER);
            t.resetValue(OptimizationRunState.FITNESS_VALUES);
            t.resetValue(OptimizationRunState.EVALUATION_NUMBER);
            t.resetValue(OptimizationRunState.USED_CONNECTIONS);
            t.resetValue(OptimizationRunState.USED_HIDDEN_NODES);
        });
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

    private void exportSummary() {
        this.generalState.execute(t -> {
            t.export(OptimizationState.EVALUATION_DISTRIBUTION);
            t.export(OptimizationState.FITNESS_DISTRIBUTION);
            t.export(OptimizationState.HIDDEN_NODES_DISTRIBUTION);
            t.export(OptimizationState.CONNECTIONS_DISTRIBUTION);
            t.export(OptimizationState.FINISHED_RUN_DISTRIBUTION);
            t.export(OptimizationState.GLOBAL_BEST_SOLUTION);
        });
    }

    private void incrementGenerationCounter() {
        this.generationCounter++;
        LOG.info("Generating solutions for iteration {}", this.generationCounter);
    }

    protected void incrementRunCounter() {
        this.runCounter++;
        this.runState.setName("RUN_" + this.runCounter);
        this.runState.execute(t -> t.addNewValue(OptimizationRunState.RUN_NUMBER, this.runCounter));
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

    protected void updateState() {
        List<Double> fitnessValues = new ArrayList<>();
        List<Double> hiddenNodeNumbers = new ArrayList<>();
        List<Double> connectionNumbers = new ArrayList<>();

        for (Solution solution : getCurrentSolutions()) {
            double fitness = solution.getFitness();
            double hiddenNodeCount = NeuralNetworkUtil.countHiddenNodes(solution.getNeuralNetwork());
            double connectionCount = NeuralNetworkUtil.countConnections(solution.getNeuralNetwork());

            fitnessValues.add(fitness);
            hiddenNodeNumbers.add(hiddenNodeCount);
            connectionNumbers.add(connectionCount);
            this.bestFitness = Double.max(this.bestFitness, fitness);
        }

        this.runState.execute(t -> {
            t.addNewValue(OptimizationRunState.FITNESS_VALUE, this.bestFitness);
            t.addNewValue(OptimizationRunState.FITNESS_VALUES, fitnessValues);
            t.addNewValue(OptimizationRunState.USED_HIDDEN_NODES, hiddenNodeNumbers);
            t.addNewValue(OptimizationRunState.USED_CONNECTIONS, connectionNumbers);
        });

        this.averageHandler.addFitnessSummary(getEvaluationCounter(), getFitnessOfIteration());
    }

    protected void updateBestSolution() {
        GraphStateValue.GraphData graphData = GraphHelper.retrieveGraph(this.currentlyBestSolution.getNeuralNetwork());

        if (isBestOverAllSolution()) {
            this.overallBestSolution = this.currentlyBestSolution;
            this.generalState.execute(t -> t.addNewValue(OptimizationState.GLOBAL_BEST_SOLUTION, graphData));
        }

        this.runState.execute(t -> t.addNewValue(OptimizationRunState.CURRENT_BEST_SOLUTION, graphData));
    }

    private boolean isBestOverAllSolution() {
        if (this.overallBestSolution == null) {
            return true;
        }

        if (this.currentlyBestSolution.getFitness() < this.overallBestSolution.getFitness()) {
            return false;
        }

        return NeuralNetworkUtil.isSmaller(this.currentlyBestSolution.getNeuralNetwork(), this.overallBestSolution.getNeuralNetwork());
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
        if (currentBest == null || currentBest.getFitness() < this.bestFitness) {
            return;
        }

        if (this.currentlyBestSolution == null || NeuralNetworkUtil.isSmaller(currentBest.getNeuralNetwork(), this.currentlyBestSolution.getNeuralNetwork())) {
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

    public int getRunCounter() {
        return this.runCounter;
    }

    public ConfigurationHandler<OptimizationConfiguration> getOptimizationConfiguration() {
        return this.configuration;
    }

    public StateHandler<OptimizationState> getState() {
        return this.generalState;
    }

}
