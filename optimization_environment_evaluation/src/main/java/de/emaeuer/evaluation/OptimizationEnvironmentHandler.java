package de.emaeuer.evaluation;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.factory.EnvironmentFactory;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.factory.OptimizationMethodFactory;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class OptimizationEnvironmentHandler implements Runnable {

    private static final Logger LOG = LogManager.getLogger(OptimizationEnvironmentHandler.class);

    private int maxEvaluations = 1;
    private int maxRuns = 1;
    private int maxTime = 0;
    private double maxFitness = 1;

    private int evaluationCounter = 0;
    private int runCounter = 0;
    private double fitness = 0;

    private boolean finished = false;
    private boolean updateNotifier = false;
    private boolean automaticallyStartNextRun = true;
    private boolean currentlyAutomaticallyPaused = false;
    private boolean stoppedBecauseOfException = false;

    private StateHandler<OptimizationState> optimizationState;
    private ConfigurationHandler<EvaluationConfiguration> configuration;

    private AbstractEnvironment<?> environment;
    private OptimizationMethod optimization;

    private final Lock updateLock = new ReentrantLock(true);

    private final AtomicInteger updateDelta = new AtomicInteger(1000 / 60);
    private final AtomicBoolean terminateThread = new AtomicBoolean(false);
    private final AtomicBoolean pauseThread = new AtomicBoolean(false);

    private Thread updateThread;

    public void initialize() {
        this.maxTime = this.configuration.getValue(EvaluationConfiguration.MAX_TIME, Integer.class);

        if (this.configuration != null) {
            createEnvironment();
        } else {
            LOG.warn("The configuration was not set");
            throw new IllegalStateException("The configuration was not set");
        }

        if (this.optimizationState != null) {
            createOptimizationMethod();
        } else {
            LOG.warn("The optimization state was not set");
            throw new IllegalStateException("The optimization state was not set");
        }
    }

    private void createEnvironment() {
        this.maxFitness = this.configuration.getValue(EvaluationConfiguration.MAX_FITNESS_SCORE, Double.class);
        ConfigurationHandler<EnvironmentConfiguration> environmentConfig = ConfigurationHelper.extractEmbeddedConfiguration(this.configuration, EnvironmentConfiguration.class, EvaluationConfiguration.ENVIRONMENT_CONFIGURATION);
        this.environment = EnvironmentFactory.createEnvironment(environmentConfig);
    }

    private void createOptimizationMethod() {
        ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(this.configuration, OptimizationConfiguration.class, EvaluationConfiguration.OPTIMIZATION_CONFIGURATION);

        this.maxEvaluations = optimizationConfig.getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class);
        this.maxRuns = optimizationConfig.getValue(OptimizationConfiguration.NUMBER_OF_RUNS, Integer.class);
        this.optimization = OptimizationMethodFactory.createMethodForConfig(optimizationConfig, this.optimizationState);
    }

    public void reset() {
        this.optimization = null;
        this.environment = null;

        this.evaluationCounter = 0;
        this.runCounter = 0;
        this.fitness = 0;
        this.finished = false;
        this.stoppedBecauseOfException = false;

        this.terminateThread.set(false);
    }

    public void update() {
        if (environment.environmentFinished()) {
            handleRestart();
            this.updateNotifier = !this.updateNotifier;
        }

        if (!this.finished && !isPaused()) {
            step();
        }
    }

    private void handleRestart() {
        // next iteration starts only if the generalization was finished
        boolean startedGeneralization = startGeneralizationIfNecessary();
        if (startedGeneralization) {
            return;
        } else if (this.environment.isTestingGeneralization()) {
            this.environment.nextGeneralizationIteration();
            return;
        }

        // optimization update only if the optimization already started
        if (optimization.getEvaluationCounter() > 0) {
            this.optimization.update();
        }

        if (optimization.isRunFinished() && !this.automaticallyStartNextRun && !this.currentlyAutomaticallyPaused) {
            // wait for user to continue with the next run
            this.pauseThread.set(true);
            this.currentlyAutomaticallyPaused = true;
        } else if (optimization.isOptimizationFinished()) {
            this.optimization.updateAfterRunEnd();
            handleEnd();
        } else if (optimization.isRunFinished()) {
            this.optimization.updateAfterRunEnd();
            handleNextRun();
            this.currentlyAutomaticallyPaused = false;
        } else {
            handleNextIteration();
        }
    }

    private void handleEnd() {
        this.terminateThread.set(true);
    }

    private void handleNextRun() {
        this.optimization.resetAndRestart();
    }

    private void handleNextIteration() {
        List<? extends Solution> neuralNetworksToEvaluate = this.optimization.nextIteration();

        List<AgentController> solutions = neuralNetworksToEvaluate
                .stream()
                .map(NeuralNetworkAgentController::new)
                .collect(Collectors.toList());

        // can be removed for normal usage but protects the system from freezing during irace runs
        int max = neuralNetworksToEvaluate.stream()
                .map(Solution::getNeuralNetwork)
                .mapToInt(NeuralNetwork::getNumberOfHiddenNeurons)
                .max()
                .orElse(0);

//        if (max > 20) {
//            throw new IllegalStateException("Network got to large, aborting optimization");
//        }

        this.environment.setControllers(solutions);
    }

    private boolean startGeneralizationIfNecessary() {
        boolean mustStartGeneralization = this.configuration.getValue(EvaluationConfiguration.TEST_GENERALIZATION, Boolean.class)
            && this.environment.controllerFinishedWithoutDying()
            && !this.environment.finishedGeneralization()
            && !this.environment.isTestingGeneralization();

        if (mustStartGeneralization) {
            this.environment.testGeneralization();
        }

        return mustStartGeneralization;
    }

    private void step() {
        this.environment.step();
    }

    public void startThreat() {
        this.pauseThread.set(false);

        this.updateThread = new Thread(this);
        this.updateThread.setDaemon(true);
        this.updateThread.start();
    }

    public void pauseThread() {
        this.pauseThread.set(true);
        try {
            if (this.updateThread != null) {
                this.updateThread.join();
            }
        } catch (InterruptedException e) {
            LOG.warn("Failed to pause thread", e);
        }
    }

    @Override
    public void run() {
        try {
            double startTime = System.currentTimeMillis();
            while (!this.terminateThread.get() && !this.pauseThread.get()) {
                this.updateLock.lock();
                try {
                    update();

                    if (this.maxTime > 0 && System.currentTimeMillis() - startTime > this.maxTime) {
                        // if the max time was surpassed by one hour the process was presumably put to sleep and should proceed
                        // an iteration that takes over one hour is not expected
                        if (System.currentTimeMillis() - startTime > this.maxTime + 3600000) {
                            startTime = System.currentTimeMillis();
                        } else {
                            throw new TimeoutException("Optimization took to long");
                        }
                    }
                } finally {
                    this.updateLock.unlock();
                }
                if (updateDelta.get() > 0) {
                    Thread.sleep(updateDelta.get());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn("Unexpected exception in update thread", e);
            this.stoppedBecauseOfException = true;
        }
    }

    public void refreshProperties() {
        if (this.optimization == null) {
            this.evaluationCounter = 0;
            this.runCounter = 0;
            this.fitness = 0;
            this.finished = false;
        } else {
            this.updateLock.lock();
            this.evaluationCounter = this.optimization.getEvaluationCounter();
            this.runCounter = this.optimization.getRunCounter();
            this.fitness = this.optimization.getBestFitness();
            this.finished = this.optimization.isOptimizationFinished();
            this.updateLock.unlock();
        }
    }

    public List<AbstractElement> getAgents() {
        return this.environment.getAgentsToDraw();
    }

    public List<AbstractElement> getAdditionalEnvironmentElements() {
        return this.environment.getAdditionalEnvironmentElements();
    }

    public void setUpdateDelta(int value) {
        this.updateDelta.set(value);
    }

    public void setOptimizationState(StateHandler<OptimizationState> optimizationState) {
        this.optimizationState = optimizationState;
    }

    public void setConfiguration(ConfigurationHandler<EvaluationConfiguration> configuration) {
        this.configuration = configuration;
    }

    public int getMaxRuns() {
        return maxRuns;
    }

    public int getRunCounter() {
        return runCounter;
    }

    public int getEvaluationCounter() {
        return evaluationCounter;
    }

    public int getMaxEvaluations() {
        return maxEvaluations;
    }

    public double getFitness() {
        return fitness;
    }

    public double getMaxFitness() {
        return maxFitness;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isUpdateNotifier() {
        return updateNotifier;
    }

    public void setAutomaticallyStartNextRun(boolean automaticallyStartNextRun) {
        this.automaticallyStartNextRun = automaticallyStartNextRun;
    }

    public boolean isPaused() {
        return this.pauseThread.get();
    }

    public boolean stoppedBecauseOfException() {
        return stoppedBecauseOfException;
    }

    public boolean isTestingGeneralization() {
        return this.environment != null && this.environment.isTestingGeneralization();
    }

    public double getGeneralizationCapability() {
        return this.environment == null
                ? 0
                : this.environment.getCurrentGeneralizationProgress();
    }
}
