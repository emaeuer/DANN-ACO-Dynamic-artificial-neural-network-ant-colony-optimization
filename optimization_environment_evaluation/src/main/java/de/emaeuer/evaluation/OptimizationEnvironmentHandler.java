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
import de.emaeuer.persistence.SingletonDataExporter;
import de.emaeuer.state.StateHandler;
import javafx.beans.property.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class OptimizationEnvironmentHandler {

    // TODO this should manage a javafx task to decouple the gui and optimization task

    private static final Logger LOG = LogManager.getLogger(OptimizationEnvironmentHandler.class);

    private final IntegerProperty maxEvaluations = new SimpleIntegerProperty(1);
    private final IntegerProperty maxRuns = new SimpleIntegerProperty(1);
    private final DoubleProperty maxFitness = new SimpleDoubleProperty(1);

    private final IntegerProperty evaluationCounter = new SimpleIntegerProperty(0);
    private final IntegerProperty runCounter = new SimpleIntegerProperty(0);
    private final DoubleProperty fitness = new SimpleDoubleProperty(0);

    private final BooleanProperty finished = new SimpleBooleanProperty(false);

    private final ObjectProperty<StateHandler<OptimizationState>> optimizationState = new SimpleObjectProperty<>();
    private final ObjectProperty<ConfigurationHandler<OptimizationConfiguration>> optimizationConfiguration = new SimpleObjectProperty<>();
    private final ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> environmentConfiguration = new SimpleObjectProperty<>();

    private AbstractEnvironment environment;
    private OptimizationMethod optimization;

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

        initDataExporter();
    }

    private void initDataExporter() {
        SingletonDataExporter.reset();
        SingletonDataExporter.exportConfiguration("optimization_configuration", this.optimizationConfiguration.get());
        SingletonDataExporter.exportConfiguration("environment_configuration", this.environmentConfiguration.get());
    }

    private void createEnvironment() {
        this.maxFitness.set(this.environmentConfiguration.get().getValue(EnvironmentConfiguration.MAX_FITNESS_SCORE, Double.class));
        this.environment = EnvironmentFactory.createEnvironment(this.environmentConfiguration.get());
    }

    private void createOptimizationMethod() {
        this.maxEvaluations.set(this.optimizationConfiguration.get().getValue(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS, Integer.class));
        this.maxRuns.set(this.optimizationConfiguration.get().getValue(OptimizationConfiguration.NUMBER_OF_RUNS, Integer.class));
        this.optimization = OptimizationMethodFactory.createMethodForConfig(this.optimizationConfiguration.get(), this.optimizationState.get());
    }

    public void reset() {
        this.optimization = null;
        this.environment = null;

        evaluationCounter.set(0);
        runCounter.set(0);
        fitness.set(0);
        finished.set(false);
    }

    public void update() {
        if (environment.allAgentsFinished()) {
            handleRestart();
        }

        if (!this.finished.get()) {
            step();
        }
    }

    private void handleRestart() {
        // optimization update only if the first iteration finished
        if (evaluationCounter.get() > 0) {
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
        exportRunData();
        exportData();
        this.finished.set(true);
    }

    private void handleNextRun() {
        exportRunData();
        this.optimization.resetAndRestart();
        this.runCounter.set(this.runCounter.get() + 1);
        this.evaluationCounter.set(0);
        this.fitness.set(0);
    }

    private void exportRunData() {
        SingletonDataExporter.addRunData(OptimizationState.CURRENT_ITERATION, optimization.getState());
        SingletonDataExporter.addRunData(OptimizationState.CURRENT_RUN, optimization.getState());
        SingletonDataExporter.addRunData(OptimizationState.FITNESS_VALUE, optimization.getState());
        SingletonDataExporter.finishRun();
    }

    private void exportData() {
        SingletonDataExporter.addData(OptimizationState.AVERAGE_ITERATIONS, optimization.getState());
        SingletonDataExporter.addData(OptimizationState.AVERAGE_HIDDEN_NODES, optimization.getState());
        SingletonDataExporter.addData(OptimizationState.AVERAGE_FITNESS, optimization.getState());
        SingletonDataExporter.addData(OptimizationState.AVERAGE_CONNECTIONS, optimization.getState());
        SingletonDataExporter.finishAndExport();
    }

    private void handleNextIteration() {
        List<AgentController> solutions = this.optimization.nextIteration()
                .stream()
                .map(NeuralNetworkAgentController::new)
                .collect(Collectors.toList());
        this.environment.setControllers(solutions);
        this.evaluationCounter.set(this.evaluationCounter.get() + solutions.size());
        this.fitnessProperty().set(this.optimization.getBestFitness());
    }

    private void step() {
        if (!this.environment.allAgentsFinished()) {
            this.environment.step();
        }
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
}
