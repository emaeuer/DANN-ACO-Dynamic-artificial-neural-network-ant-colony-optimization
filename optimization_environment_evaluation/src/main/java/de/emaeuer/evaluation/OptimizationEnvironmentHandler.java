package de.emaeuer.evaluation;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.factory.EnvironmentFactory;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.factory.OptimizationMethodFactory;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class OptimizationEnvironmentHandler implements Runnable {

    private static final Logger LOG = LogManager.getLogger(OptimizationEnvironmentHandler.class);

    private int maxEvaluations = 1;
    private int maxRuns = 1;
    private double maxFitness = 1;

    private int evaluationCounter = 0;
    private int runCounter = 0;
    private double fitness = 0;

    private boolean finished = false;
    private boolean updateNotifier = false;

    private StateHandler<OptimizationState> optimizationState;
    private ConfigurationHandler<OptimizationConfiguration> optimizationConfiguration;
    private ConfigurationHandler<EnvironmentConfiguration> environmentConfiguration;

    private AbstractEnvironment environment;
    private OptimizationMethod optimization;

    private final Lock updateLock = new ReentrantLock(true);

    private final AtomicInteger updateDelta = new AtomicInteger(1000 / 60);
    private final AtomicBoolean terminateThread = new AtomicBoolean(false);
    private final AtomicBoolean pauseThread = new AtomicBoolean(false);

    private Thread updateThread;

    public void initialize() {
        if (this.environmentConfiguration != null) {
            createEnvironment();
        } else {
            LOG.warn("The environment configuration was not set");
            throw new IllegalStateException("The environment configuration was not set");
        }

        if (this.optimizationState != null && this.optimizationConfiguration != null) {
            createOptimizationMethod();
        } else {
            LOG.warn("The optimization state or the optimization configuration was not set");
            throw new IllegalStateException("The optimization state or the optimization configuration was not set");
        }
    }

    private void createEnvironment() {
        this.maxFitness = this.environmentConfiguration.getValue(EnvironmentConfiguration.MAX_FITNESS_SCORE, Double.class);
        this.environment = EnvironmentFactory.createEnvironment(this.environmentConfiguration);
    }

    private void createOptimizationMethod() {
        // FIXME there may be a better solution then disabling the option in the gui and overriding it here
        // difficulty is caused by independence of the environment and optimization
        this.optimizationConfiguration.setValue(OptimizationConfiguration.MAX_FITNESS_SCORE, this.maxFitness);
        this.maxEvaluations = this.optimizationConfiguration.getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class);
        this.maxRuns = this.optimizationConfiguration.getValue(OptimizationConfiguration.NUMBER_OF_RUNS, Integer.class);
        this.optimization = OptimizationMethodFactory.createMethodForConfig(this.optimizationConfiguration, this.optimizationState);
    }

    public void reset() {
        this.optimization = null;
        this.environment = null;

        this.evaluationCounter = 0;
        this.runCounter = 0;
        this.fitness = 0;
        this.finished = false;

        this.terminateThread.set(false);
    }

    public void update() {
        if (environment.allAgentsFinished()) {
            handleRestart();
            this.updateNotifier = !this.updateNotifier;
        }

        if (!this.finished) {
            step();
        }
    }

    private void handleRestart() {
        // optimization update only if the optimization already started
        if (optimization.getEvaluationCounter() > 0) {
            this.optimization.update();
        }

        if (optimization.isOptimizationFinished()) {
            handleEnd();
        } else if (optimization.isRunFinished()) {
            handleNextRun();
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
        List<AgentController> solutions = this.optimization.nextIteration()
                .stream()
                .map(NeuralNetworkAgentController::new)
                .collect(Collectors.toList());
        this.environment.setControllers(solutions);
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
            this.updateThread.join();
        } catch (InterruptedException e) {
            LOG.warn("Failed to pause thread", e);
        }
    }

    @Override
    public void run() {
        try {
            while (!this.terminateThread.get() && !this.pauseThread.get()) {
                this.updateLock.lock();
                try {
                    update();
                } finally {
                    this.updateLock.unlock();
                }
                if (updateDelta.get() > 0) {
                    Thread.sleep(updateDelta.get());
                }
            }
        } catch (Exception e) {
            LOG.warn("Unexpected exception in update thread", e);
        }
    }

    public void refreshProperties() {
        this.updateLock.lock();
        this.evaluationCounter = this.optimization.getEvaluationCounter();
        this.runCounter = this.optimization.getRunCounter();
        this.fitness = this.optimization.getBestFitness();
        this.finished = this.optimization.isOptimizationFinished();
        this.updateLock.unlock();
    }

    public List<AbstractElement> getAgents() {
        return this.environment.getAgents();
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

    public void setOptimizationConfiguration(ConfigurationHandler<OptimizationConfiguration> optimizationConfiguration) {
        this.optimizationConfiguration = optimizationConfiguration;
    }

    public void setEnvironmentConfiguration(ConfigurationHandler<EnvironmentConfiguration> environmentConfiguration) {
        this.environmentConfiguration = environmentConfiguration;
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
}
