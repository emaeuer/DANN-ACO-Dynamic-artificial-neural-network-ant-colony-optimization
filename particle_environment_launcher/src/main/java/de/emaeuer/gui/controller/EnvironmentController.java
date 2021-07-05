package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.evaluation.EvaluationConfiguration;
import de.emaeuer.evaluation.OptimizationEnvironmentHandler;
import de.emaeuer.gui.controller.util.ShapeDrawer;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class EnvironmentController {

    private static final String SPEED_TEXT = "%.1fx";

    @FXML
    private Label nonVisualTitle;

    @FXML
    private VBox nonVisualPanel;

    @FXML
    private Canvas canvas;

    @FXML
    public ProgressBar evaluationProgress;

    @FXML
    public ProgressBar fitnessProgress;

    @FXML
    public ProgressBar runProgress;

    private final OptimizationEnvironmentHandler handler = new OptimizationEnvironmentHandler();

    private AnimationTimer frameTimer;

    private double speed = 1;
    private boolean initialStart = true;

    private final ObjectProperty<ConfigurationHandler<EvaluationConfiguration>> configuration = new SimpleObjectProperty<>();
    private final ObjectProperty<StateHandler<OptimizationState>> optimizationState = new SimpleObjectProperty<>();

    private final BooleanProperty updatedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty singleEntityMode = new SimpleBooleanProperty(false);
    private final BooleanProperty stopAfterEachRun = new SimpleBooleanProperty(false);
    private final BooleanProperty finishedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty runningProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty visualMode = new SimpleBooleanProperty(true);

    private final StringProperty speedProperty = new SimpleStringProperty(String.format(SPEED_TEXT, this.speed));

    @FXML
    public void initialize() {
        this.frameTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                nextFrame();
            }
        };
    }

    public void initializeController() {
        if (this.configuration.isNotNull().get() && this.optimizationState.isNotNull().get()) {
            this.handler.setConfiguration(this.configuration.get());
            this.handler.setOptimizationState(this.optimizationState.get());
            this.handler.setAutomaticallyStartNextRun(this.stopAfterEachRun.not().get());

            this.configuration.addListener((v, o, n) -> this.handler.setConfiguration(n));
            this.optimizationState.addListener((v, o, n) -> this.handler.setOptimizationState(n));

            this.canvas.setWidth(800);
            this.canvas.setHeight(800);

            this.finishedProperty.addListener((v,o,n) -> finished(n));
            this.nonVisualPanel.visibleProperty().bind(this.visualMode.not().or(this.finishedProperty));
            this.visualMode.addListener((v,o,n) -> changeVisualMode(n));
            this.stopAfterEachRun.addListener((v,o,n) -> this.handler.setAutomaticallyStartNextRun(!n));
        }
    }

    private void finished(boolean isFinished) {
        if (isFinished) {
            this.nonVisualTitle.setText("Finished optimization - Restart necessary");
        } else {
            this.nonVisualTitle.setText("Optimization in progress - Visual output disabled");
        }
    }

    private void changeVisualMode(boolean isVisual) {
        if (isVisual) {
            this.handler.setUpdateDelta((int) (1000 / (60 * speed)));
        } else {
            this.handler.setUpdateDelta(0);
        }
    }

    protected void nextFrame() {
        if (this.visualMode.get()) {
            // update environment multiple times to increase speed
            if (this.finishedProperty.get()) {
                getGraphicsContext().clearRect(0, 0, getGraphicsContext().getCanvas().getWidth(), getGraphicsContext().getCanvas().getHeight());
            } else {
                drawContent();
            }
        }
        refreshProperties();
    }

    private void refreshProperties() {
        this.runningProperty.set(!this.handler.isPaused());

        this.handler.refreshProperties();

        this.evaluationProgress.progressProperty().set(((double) this.handler.getEvaluationCounter()) / this.handler.getMaxEvaluations());
        this.fitnessProgress.progressProperty().set(this.handler.getFitness() / this.handler.getMaxFitness());
        this.finishedProperty.set(this.handler.isFinished());

        if (this.handler.isFinished()) {
            this.updatedProperty.set(this.updatedProperty.not().get());
            this.runProgress.progressProperty().set(1);
        } else {
            this.updatedProperty.set(this.handler.isUpdateNotifier());
            this.runProgress.progressProperty().set(Math.max(this.handler.getRunCounter() - 1.0, 0) / this.handler.getMaxRuns());
        }
    }

    private void drawContent() {
        getGraphicsContext().clearRect(0, 0, getGraphicsContext().getCanvas().getWidth(), getGraphicsContext().getCanvas().getHeight());

        synchronized (this.handler.getAgents()) {
            this.handler.getAgents()
                    .stream()
                    .limit(this.singleEntityMode.get() ? 1 : Long.MAX_VALUE)
                    .forEach(e -> ShapeDrawer.drawElement(e, getGraphicsContext()));
        }

        synchronized (this.handler.getAdditionalEnvironmentElements()) {
            this.handler.getAdditionalEnvironmentElements()
                    .forEach(e -> ShapeDrawer.drawElement(e, getGraphicsContext()));
        }
    }

    public void startEnvironment() {
        if (this.runningProperty.get()) {
            return;
        } else if (initialStart) {
            this.handler.initialize();
            this.initialStart = false;
        }

        this.runningProperty.set(true);
        this.handler.startThreat();

        this.frameTimer.start();
    }

    public void pauseEnvironment() {
        this.runningProperty.set(false);
        this.handler.pauseThread();
        this.frameTimer.stop();
    }

    public void restartEnvironment() {
        if (this.runningProperty.get()) {
            pauseEnvironment();
        }

        this.initialStart = true;
        this.handler.reset();
        refreshProperties();

        getGraphicsContext().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void increaseEnvironmentSpeed() {
        speed += speed >= 10 ? 0 : 0.5; // maximum speed is 10
        this.handler.setUpdateDelta((int) (1000 / (60 * speed)));
        changeSpeedText();
    }

    public void decreaseEnvironmentSpeed() {
        speed -= speed <= 0.5 ? 0 : 0.5; // minimum speed is 0.5
        this.handler.setUpdateDelta((int) (1000 / (60 * speed)));
        changeSpeedText();
    }

    private void changeSpeedText() {
        this.speedProperty.setValue(String.format(SPEED_TEXT, this.speed));
    }


    protected GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    public ObjectProperty<ConfigurationHandler<EvaluationConfiguration>> configurationProperty() {
        return this.configuration;
    }

    public ObjectProperty<StateHandler<OptimizationState>> optimizationStateProperty() {
        return optimizationState;
    }

    public BooleanProperty updatedProperty() {
        return this.updatedProperty;
    }

    public StringProperty speedProperty() {
        return this.speedProperty;
    }

    public void toggleEntityMode() {
        this.singleEntityMode.set(this.singleEntityMode.not().get());
    }

    public boolean isSingleEntityMode() {
        return singleEntityMode.get();
    }

    public void toggleVisualMode() {
        this.visualMode.set(this.visualMode.not().get());
        // clear screen
        this.canvas.getGraphicsContext2D().clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    }

    public boolean isVisualMode() {
        return this.visualMode.get();
    }

    public BooleanProperty finishedProperty() {
        return this.finishedProperty;
    }

    public void toggleStopAfterEachIteration() {
        this.stopAfterEachRun.set(this.stopAfterEachRun.not().get());
    }

    public boolean getStopAfterEachRun() {
        return this.stopAfterEachRun.get();
    }

    public BooleanProperty runningProperty() {
        return runningProperty;
    }
}
