package de.emaeuer.optimization;

import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationRunState;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.util.GraphHelper;
import de.emaeuer.optimization.util.ProgressionHandler;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.optimization.util.RunDataHandler;
import de.emaeuer.optimization.util.RunDataHandler.RunSummary;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.GraphStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(OptimizationMethod.class);

    private int runCounter = 0;
    private int generationCounter = 0;
    private int evaluationCounter = 0;

    private final ConfigurationHandler<OptimizationConfiguration> configuration;
    private final StateHandler<OptimizationState> generalState;
    private final StateHandler<OptimizationRunState> runState;

    private Solution currentlyBestSolution;
    private Solution overallBestSolution;

    private ProgressionHandler progressionHandler;
    private final RunDataHandler averageHandler;

    private boolean optimizationFinished = false;
    private boolean runFinished = false;

    private final RandomUtil rng;

    protected OptimizationMethod(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        this.configuration = configuration;
        this.generalState = generalState;
        this.runState = new StateHandler<>(OptimizationRunState.class, generalState);
        this.generalState.execute(t -> t.addNewValue(OptimizationState.STATE_OF_CURRENT_RUN, this.runState));
        this.rng = new RandomUtil(configuration.getValue(OptimizationConfiguration.SEED, Integer.class));

        this.averageHandler = new RunDataHandler(this.generalState, this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class));

        this.configuration.logConfiguration();

        incrementRunCounter();

        this.progressionHandler = new ProgressionHandler(
                configuration.getValue(OptimizationConfiguration.PROGRESSION_ITERATIONS, Integer.class),
                configuration.getValue(OptimizationConfiguration.PROGRESSION_THRESHOLD, Double.class));
    }

    public List<? extends Solution> nextIteration() {
        // do nothing if the optimization finished (maximum fitness or max number of evaluations reached)
        if (this.optimizationFinished) {
            return Collections.emptyList();
        }

        this.generationCounter++;

        List<? extends Solution> solutions = generateSolutions();
        LOG.info("Generated {} solutions for iteration {}", solutions.size(), this.generationCounter);

        this.evaluationCounter += solutions.size();
        this.runState.execute(t -> t.addNewValue(OptimizationRunState.EVALUATION_NUMBER, this.evaluationCounter));

        return solutions;
    }

    public void update() {
        // do nothing if the optimization finished (maximum fitness or max number of evaluations reached)
        if (this.optimizationFinished || this.evaluationCounter == 0) {
            return;
        }

        LOG.info("Executing update for iteration {}", this.generationCounter);
        updateState();

        LOG.info("Best found solution of iteration {} has a fitness of {}", this.generationCounter, getBestFitness());

        this.progressionHandler.addFitnessScore(getBestFitness());
        if (this.progressionHandler.doesStagnate()) {
            handleProgressionStagnation();
        }

        if (!checkCurrentRunFinished()) {
            return;
        }

        LOG.info("Finished run {} with a maximal fitness of {}", this.runCounter, getBestFitness());
        this.runFinished = true;
        this.optimizationFinished = checkOptimizationFinished();
    }

    public void updateAfterRunEnd() {
        updateGeneralState();
        updateRunState();
        updateRunStatistics();
        updateImplementationState();

        if (this.optimizationFinished) {
            LOG.info("Finished optimization with a maximal fitness of {}", this.overallBestSolution.getFitness());
            exportSummary();
        }
    }

    protected abstract void updateImplementationState();

    public void resetAndRestart() {
        incrementRunCounter();
        this.currentlyBestSolution = null;
        this.runFinished = false;
        this.generationCounter = 0;
        this.evaluationCounter = 0;

        this.progressionHandler = new ProgressionHandler(
                configuration.getValue(OptimizationConfiguration.PROGRESSION_ITERATIONS, Integer.class),
                configuration.getValue(OptimizationConfiguration.PROGRESSION_THRESHOLD, Double.class));
    }

    private void updateGeneralState() {
        boolean finishedPremature = getBestFitness() >= this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);

        this.generalState.execute(t -> {
            t.addNewValue(OptimizationState.EVALUATION_DISTRIBUTION, Integer.valueOf(this.evaluationCounter).doubleValue());
            t.addNewValue(OptimizationState.FITNESS_DISTRIBUTION, getBestFitness());
            t.addNewValue(OptimizationState.FINISHED_RUN_DISTRIBUTION, finishedPremature ? 1.0 : 0.0);
            t.addNewValue(OptimizationState.AVERAGE_GENERALIZATION_CAPABILITY, getBestGeneralizationCapability());
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
        boolean finishedPremature = getBestFitness() >= this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);

        this.runState.execute(t -> {
            t.export(OptimizationRunState.CURRENT_BEST_SOLUTION);
            t.addNewValue(OptimizationRunState.RUN_FINISHED, finishedPremature ? 1 : 0);
            t.resetValue(OptimizationRunState.FITNESS_VALUE);
            t.resetValue(OptimizationRunState.CURRENT_BEST_SOLUTION);
            t.resetValue(OptimizationRunState.RUN_FINISHED);
            t.resetValue(OptimizationRunState.RUN_NUMBER);
            t.resetValue(OptimizationRunState.FITNESS_VALUES);
            t.resetValue(OptimizationRunState.GENERALIZATION_CAPABILITY);
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

        RunSummary summary = new RunSummary(getBestFitness(), this.evaluationCounter, numberOfHiddenNodes, numberOfConnections);
        this.averageHandler.addSummaryOfRun(summary);
    }

    private void exportSummary() {
        this.generalState.execute(t -> {
            t.export(OptimizationState.EVALUATION_DISTRIBUTION);
            t.export(OptimizationState.FITNESS_DISTRIBUTION);
            t.export(OptimizationState.HIDDEN_NODES_DISTRIBUTION);
            t.export(OptimizationState.CONNECTIONS_DISTRIBUTION);
            t.export(OptimizationState.FINISHED_RUN_DISTRIBUTION);
            t.export(OptimizationState.AVERAGE_GENERALIZATION_CAPABILITY);
            t.export(OptimizationState.GLOBAL_BEST_SOLUTION);
        });
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
        boolean generalizationFinished = getBestGeneralizationCapability() >= this.configuration.getValue(OptimizationConfiguration.GENERALIZATION_CAPABILITY_THRESHOLD, Double.class);
        boolean maxFitnessReached = getBestFitness() >= this.configuration.getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);
        boolean maxEvaluationsReached = this.evaluationCounter >= this.configuration.getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class);

        if (this.configuration.getValue(OptimizationConfiguration.TEST_GENERALIZATION, Boolean.class)) {
            return (generalizationFinished && maxFitnessReached) || maxEvaluationsReached;
        } else {
            return maxFitnessReached || maxEvaluationsReached;
        }
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
        }

        this.runState.execute(t -> {
            t.addNewValue(OptimizationRunState.FITNESS_VALUE, getBestFitness());
            t.addNewValue(OptimizationRunState.GENERALIZATION_CAPABILITY, getBestGeneralizationCapability());
            t.addNewValue(OptimizationRunState.FITNESS_VALUES, fitnessValues);
            t.addNewValue(OptimizationRunState.USED_HIDDEN_NODES, hiddenNodeNumbers);
            t.addNewValue(OptimizationRunState.USED_CONNECTIONS, connectionNumbers);
        });

        this.averageHandler.addFitnessSummary(getEvaluationCounter(), getFitnessOfIteration());
    }

    protected abstract List<? extends Solution> getCurrentSolutions();

    protected abstract DoubleSummaryStatistics getFitnessOfIteration();

    protected abstract List<? extends Solution> generateSolutions();

    public int getEvaluationCounter() {
        return this.evaluationCounter;
    }

    protected void setCurrentlyBestSolution(Solution currentBest) {
        if (this.currentlyBestSolution == null || isBetter(currentBest, this.currentlyBestSolution)) {
            this.currentlyBestSolution = currentBest;
            updateBestSolution();
        }
    }

    protected void updateBestSolution() {
        GraphStateValue.GraphData graphData = GraphHelper.retrieveGraph(this.currentlyBestSolution.getNeuralNetwork());

        if (this.overallBestSolution == null || isBetter(this.currentlyBestSolution, this.overallBestSolution)) {
            this.overallBestSolution = this.currentlyBestSolution;
            this.generalState.execute(t -> t.addNewValue(OptimizationState.GLOBAL_BEST_SOLUTION, graphData));
        }

        this.runState.execute(t -> t.addNewValue(OptimizationRunState.CURRENT_BEST_SOLUTION, graphData));
    }

    private boolean isBetter(Solution a, Solution b) {
        if (a.getGeneralizationCapability() > b.getGeneralizationCapability()) {
            return true;
        } else if (a.getGeneralizationCapability() < b.getGeneralizationCapability()) {
            return false;
        }

        if (a.getFitness() > b.getFitness()) {
            return true;
        } else if (a.getFitness() < b.getFitness()) {
            return false;
        }

        return NeuralNetworkUtil.isSmaller(a.getNeuralNetwork(), b.getNeuralNetwork());
    }

    public boolean isOptimizationFinished() {
        return optimizationFinished;
    }

    public boolean isRunFinished() {
        return runFinished;
    }

    public double getBestFitness() {
        return this.currentlyBestSolution == null
                ? 0
                : this.currentlyBestSolution.getFitness();
    }

    public double getBestGeneralizationCapability() {
        return this.currentlyBestSolution == null
                ? 0
                : this.currentlyBestSolution.getGeneralizationCapability();
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

    protected RandomUtil getRNG() {
        return this.rng;
    }
}
