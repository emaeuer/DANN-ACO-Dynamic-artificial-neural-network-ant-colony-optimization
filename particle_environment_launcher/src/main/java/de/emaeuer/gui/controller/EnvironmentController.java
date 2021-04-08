package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.evaluation.OptimizationEnvironmentHandler;
import de.emaeuer.gui.controller.util.ShapeDrawer;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.NumberBinding;
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
    private boolean evenFrameNumber = false;
    private boolean running = false;
    private boolean initialStart = true;

    private final ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> environmentConfiguration = new SimpleObjectProperty<>();
    private final ObjectProperty<ConfigurationHandler<OptimizationConfiguration>> optimizationConfiguration = new SimpleObjectProperty<>();
    private final ObjectProperty<StateHandler<OptimizationState>> optimizationState = new SimpleObjectProperty<>();

    private final BooleanProperty singleEntityMode = new SimpleBooleanProperty(false);
    private final BooleanProperty finishedProperty = new SimpleBooleanProperty(false);
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
        if (this.environmentConfiguration.isNotNull().get() && this.optimizationConfiguration.isNotNull().get() && this.optimizationState.isNotNull().get()) {
            this.handler.environmentConfigurationProperty().bind(this.environmentConfiguration);
            this.handler.optimizationConfigurationProperty().bind(this.optimizationConfiguration);
            this.handler.optimizationStateProperty().bind(this.optimizationState);

            this.runProgress.progressProperty().bind(Bindings.createDoubleBinding(() ->
                            (double) this.handler.runCounterProperty().get() / this.handler.maxRunsProperty().get(),
                    this.handler.runCounterProperty(), this.handler.maxRunsProperty()));
            this.evaluationProgress.progressProperty().bind(Bindings.createDoubleBinding(() ->
                            (double) this.handler.evaluationCounterProperty().get() / this.handler.maxEvaluationsProperty().get(),
                            this.handler.evaluationCounterProperty(), this.handler.maxEvaluationsProperty()));
            this.fitnessProgress.progressProperty().bind(Bindings.createDoubleBinding(() ->
                    this.handler.fitnessProperty().get() / this.handler.maxFitnessProperty().get(),
                    this.handler.fitnessProperty(), this.handler.maxFitnessProperty()));

            this.canvas.setWidth(800);
            this.canvas.setHeight(800);

            this.finishedProperty.addListener((v,o,n) -> finished(n));
            this.finishedProperty.bind(this.handler.finishedProperty());
            this.nonVisualPanel.visibleProperty().bind(this.visualMode.not().or(this.finishedProperty));
        }
    }

    private void finished(boolean isFinished) {
        if (isFinished) {
            this.nonVisualTitle.setText("Finished optimization - Restart necessary");
        } else {
            this.nonVisualTitle.setText("Optimization in progress - Visual output disabled");
        }
    }

    protected void nextFrame() {
        // update environment multiple times to increase speed
        updateEnvironmentAccordingToSpeed();
        if (this.finishedProperty.not().get()) {
            getGraphicsContext().clearRect(0, 0, getGraphicsContext().getCanvas().getWidth(), getGraphicsContext().getCanvas().getHeight());
        } else {
            drawContent();
        }
    }

    private void drawContent() {
        getGraphicsContext().clearRect(0, 0, getGraphicsContext().getCanvas().getWidth(), getGraphicsContext().getCanvas().getHeight());

        this.handler.getAgents()
                .stream()
                .limit(this.singleEntityMode.get() ? 1 : Long.MAX_VALUE)
                .forEach(e -> ShapeDrawer.drawElement(e, getGraphicsContext()));

        this.handler.getAdditionalEnvironmentElements()
                .forEach(e -> ShapeDrawer.drawElement(e, getGraphicsContext()));
    }

    private void updateEnvironmentAccordingToSpeed() {
        this.evenFrameNumber = !this.evenFrameNumber;

        double floored = Math.floor(this.speed);
        for (int i = 0; i < floored; i++) {
            if (this.finishedProperty.get()) {
                break;
            }
            this.handler.update();
        }

        if (this.finishedProperty.get()) {
            return;
        }

        // additional iteration every second frame if speed - floor == 0.5
        if (this.evenFrameNumber && this.speed - floored > 0) {
            this.handler.update();
        }
    }

    private void nonVisualEnvironmentUpdate() {
        // JavaFX schedules next environment update and calls this method again for next update
        // no concurrency because finished runnable starts next one
        Platform.runLater(() -> {
            // can be null if this task is called after reset
            if (running && this.handler.finishedProperty().not().get() && this.visualMode.not().get()) {
                this.handler.update();
                nonVisualEnvironmentUpdate();
            }
        });
    }

    public void startEnvironment() {
        if (running) {
            return;
        } else if (initialStart) {
            this.handler.initialize();
            this.initialStart = false;
        }

        this.running = true;

        // either start visual runner or non visual runner
        if (this.visualMode.get()) {
            this.frameTimer.start();
        } else {
            this.nonVisualEnvironmentUpdate();
        }
    }

    public void pauseEnvironment() {
        this.running = false;
        this.frameTimer.stop();
    }

    public void restartEnvironment() {
        if (this.running) {
            pauseEnvironment();
        }

        this.initialStart = true;
        this.handler.reset();

        getGraphicsContext().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void increaseEnvironmentSpeed() {
        speed += speed >= 10 ? 0 : 0.5; // maximum speed is 10
        changeSpeedText();
    }

    public void decreaseEnvironmentSpeed() {
        speed -= speed <= 0.5 ? 0 : 0.5; // minimum speed is 0.5
        changeSpeedText();
    }

    private void changeSpeedText() {
        this.speedProperty.setValue(String.format(SPEED_TEXT, this.speed));
    }


    protected GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    public ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> environmentConfigurationProperty() {
        return environmentConfiguration;
    }

    public ObjectProperty<ConfigurationHandler<OptimizationConfiguration>> optimizationConfigurationProperty() {
        return optimizationConfiguration;
    }

    public ObjectProperty<StateHandler<OptimizationState>> optimizationStateProperty() {
        return optimizationState;
    }

    public BooleanProperty updatedProperty() {
        return this.handler.updatedProperty();
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

        switchVisualization();

        if (this.running) {
            // pause to stop old runner and start to execute new one
            pauseEnvironment();
            startEnvironment();
        }
    }

    private void switchVisualization() {
        this.canvas.getGraphicsContext2D().clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    }

    public boolean isVisualMode() {
        return this.visualMode.get();
    }

    public BooleanProperty finishedProperty() {
        return this.finishedProperty;
    }

}
