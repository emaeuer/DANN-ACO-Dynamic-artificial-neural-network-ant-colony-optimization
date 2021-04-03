package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.factory.EnvironmentFactory;
import de.emaeuer.gui.controller.environment.handler.EnvironmentHandler;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.util.Objects;

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

    private EnvironmentHandler<?> environmentHandler;

    private AnimationTimer frameTimer;

    private double speed = 1;
    private boolean evenFrameNumber = false;
    private boolean running = false;

    private final ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> configuration = new SimpleObjectProperty<>();
    private final ObjectProperty<StateHandler<OptimizationState>> state = new SimpleObjectProperty<>();

    private final BooleanProperty restartedProperty = new SimpleBooleanProperty();
    private final BooleanProperty singleEntityMode = new SimpleBooleanProperty(false);
    private final BooleanProperty finishedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty visualMode = new SimpleBooleanProperty(true);
    private final StringProperty speedProperty = new SimpleStringProperty(String.format(SPEED_TEXT, this.speed));

    @FXML
    public void initialize() {
        this.canvas.widthProperty().setValue(800);
        this.canvas.heightProperty().setValue(800);

        this.frameTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                nextFrame();
            }
        };
    }

    private void initializeController() {
        if (this.configuration.isNotNull().get() && this.state.isNotNull().get()) {
            resetEnvironment();

            this.environmentHandler = Objects.requireNonNull(
                    EnvironmentHandler.createEnvironmentHandler(EnvironmentFactory.createEnvironment(this.configuration.get(), this.state.get()), getGraphicsContext()));

            this.runProgress.progressProperty().bind(this.environmentHandler.runProgressProperty());
            this.evaluationProgress.progressProperty().bind(this.environmentHandler.evaluationProgressProperty());
            this.fitnessProgress.progressProperty().bind(this.environmentHandler.fitnessProgressProperty());

            this.canvas.setWidth(this.environmentHandler.getEnvironmentWidth());
            this.canvas.setHeight(this.environmentHandler.getEnvironmentHeight());
            this.restartedProperty.bind(this.environmentHandler.restartedProperty());
            this.environmentHandler.singleEntityModeProperty().bind(this.singleEntityMode);
            this.finishedProperty.addListener((v,o,n) -> finished(n));
            this.finishedProperty.bind(this.environmentHandler.finishedProperty());
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

    protected void resetEnvironment() {
        getGraphicsContext().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        this.finishedProperty.unbind();
        this.restartedProperty.unbind();

        this.environmentHandler = null;

        this.finishedProperty.set(false);
        this.restartedProperty.set(true);
    }

    protected void nextFrame() {
        // update environment multiple times to increase speed
        updateEnvironmentAccordingToSpeed();
        environmentHandler.drawContent();
    }

    private void updateEnvironmentAccordingToSpeed() {
        this.evenFrameNumber = !this.evenFrameNumber;

        double floored = Math.floor(this.speed);
        for (int i = 0; i < floored; i++) {
            environmentHandler.update();
        }

        // additional iteration every second frame if speed - floor == 0.5
        if (this.evenFrameNumber && this.speed - floored > 0) {
            environmentHandler.update();
        }
    }

    private void nonVisualEnvironmentUpdate() {
        if (running && this.environmentHandler.finishedProperty().not().get() && this.visualMode.not().get()) {
            // JavaFX schedules next environment update and calls this method again for next update
            // no concurrency because finished runnable starts next one
            Platform.runLater(() -> {
                // can be null if this task is called after reset
                if (this.environmentHandler == null) {
                    return;
                }
                this.environmentHandler.update();
                nonVisualEnvironmentUpdate();
            });
        }
    }

    public void startEnvironment() {
        // initialization necessary if environment handler isn't initialized
        if (this.environmentHandler == null) {
            initializeController();
        }

        if (running) {
            return;
        }

        this.running = true;

        // either start visual runner or non visual runner
        if (this.visualMode.get()) {
            this.frameTimer.start();
        } else {
            this.nonVisualEnvironmentUpdate();
        }
    }

    @FXML
    public void pauseEnvironment() {
        if (this.environmentHandler == null) {
            return;
        }

        this.running = false;
        this.frameTimer.stop();
    }

    @FXML
    public void restartEnvironment() {
        if (this.running) {
            pauseEnvironment();
        }

        resetEnvironment();
    }

    @FXML
    public void increaseEnvironmentSpeed() {
        speed += speed >= 10 ? 0 : 0.5; // maximum speed is 10
        changeSpeedText();
    }

    @FXML
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

    public ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> configurationProperty() {
        return configuration;
    }

    public ObjectProperty<StateHandler<OptimizationState>> stateProperty() {
        return state;
    }

    public BooleanProperty restartedProperty() {
        return this.restartedProperty;
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
