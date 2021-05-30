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
import javafx.application.Platform;
import javafx.beans.property.*;
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

    private final IntegerProperty maxEvaluations = new SimpleIntegerProperty(1);
    private final IntegerProperty maxRuns = new SimpleIntegerProperty(1);
    private final DoubleProperty maxFitness = new SimpleDoubleProperty(1);

    private final IntegerProperty evaluationCounter = new SimpleIntegerProperty(0);
    private final IntegerProperty runCounter = new SimpleIntegerProperty(0);
    private final DoubleProperty fitness = new SimpleDoubleProperty(0);

    private final BooleanProperty finished = new SimpleBooleanProperty(false);
    // property which gets toggled every time something changed
    private final BooleanProperty updateNotifier = new SimpleBooleanProperty(false);

    private final ObjectProperty<StateHandler<OptimizationState>> optimizationState = new SimpleObjectProperty<>();
    private final ObjectProperty<ConfigurationHandler<OptimizationConfiguration>> optimizationConfiguration = new SimpleObjectProperty<>();
    private final ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> environmentConfiguration = new SimpleObjectProperty<>();

    private AbstractEnvironment environment;
    private OptimizationMethod optimization;

    private final Lock updateLock = new ReentrantLock(true);

    private final AtomicInteger updateDelta = new AtomicInteger(1000 / 60);
    private final AtomicBoolean terminateThread = new AtomicBoolean(false);
    private final AtomicBoolean pauseThread = new AtomicBoolean(false);

    private Thread updateThread;

    public void initialize() {
        if (this.environmentConfiguration.isNotNull().get()) {
            createEnvironment();
        } else {
            LOG.warn("The environment configuration was not set");
            throw new IllegalStateException("The environment configuration was not set");
        }

        if (this.optimizationState.isNotNull().get() && this.optimizationConfiguration.isNotNull().get()) {
            createOptimizationMethod();
        } else {
            LOG.warn("The optimization state or the optimization configuration was not set");
            throw new IllegalStateException("The optimization state or the optimization configuration was not set");
        }
    }

    private void createEnvironment() {
        this.maxFitness.set(this.environmentConfiguration.get().getValue(EnvironmentConfiguration.MAX_FITNESS_SCORE, Double.class));
        this.environment = EnvironmentFactory.createEnvironment(this.environmentConfiguration.get());
    }

    private void createOptimizationMethod() {
        // FIXME there may be a better solution then disabling the option in the gui and overriding it here
        // difficulty is caused by independence of the environment and optimization
        this.optimizationConfiguration.get().setValue(OptimizationConfiguration.MAX_FITNESS_SCORE, this.maxFitness.get());
        this.maxEvaluations.set(this.optimizationConfiguration.get().getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class));
        this.maxRuns.set(this.optimizationConfiguration.get().getValue(OptimizationConfiguration.NUMBER_OF_RUNS, Integer.class));
        this.optimization = OptimizationMethodFactory.createMethodForConfig(this.optimizationConfiguration.get(), this.optimizationState.get());
    }

    public void reset() {
        this.optimization = null;
        this.environment = null;

        this.evaluationCounter.set(0);
        this.runCounter.set(0);
        this.fitness.set(0);
        this.finished.set(false);

        this.terminateThread.set(false);
    }

    public void update() {
        if (environment.allAgentsFinished()) {
            handleRestart();
            Platform.runLater(() -> this.updateNotifier.set(!this.updateNotifier.get()));
        }

        if (!this.finished.get()) {
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
        this.evaluationCounter.set(this.optimization.getEvaluationCounter());
        this.runCounter.set(this.optimization.getRunCounter());
        this.fitness.set(this.optimization.getBestFitness());
        this.finished.set(this.optimization.isOptimizationFinished());
        this.updateLock.unlock();
    }

    public ObjectProperty<StateHandler<OptimizationState>> optimizationStateProperty() {
        return optimizationState;
    }

    public ObjectProperty<ConfigurationHandler<OptimizationConfiguration>> optimizationConfigurationProperty() {
        return optimizationConfiguration;
    }

    public ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> environmentConfigurationProperty() {
        return environmentConfiguration;
    }

    public BooleanProperty updatedProperties() {return updateNotifier;}

    public BooleanProperty finishedProperty() {
        return finished;
    }

    public IntegerProperty maxEvaluationsProperty() {
        return maxEvaluations;
    }

    public IntegerProperty evaluationCounterProperty() {
        return evaluationCounter;
    }

    public IntegerProperty maxRunsProperty() {
        return maxRuns;
    }

    public IntegerProperty runCounterProperty() {
        return runCounter;
    }

    public DoubleProperty maxFitnessProperty() {
        return maxFitness;
    }

    public DoubleProperty fitnessProperty() {
        return fitness;
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

}
