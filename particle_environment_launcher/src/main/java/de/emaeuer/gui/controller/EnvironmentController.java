package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.factory.EnvironmentFactory;
import de.emaeuer.gui.controller.environment.handler.EnvironmentHandler;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.Objects;

public class EnvironmentController {

    private static final String SPEED_TEXT = "%.1fx";

    @FXML
    private Canvas canvas;

    private EnvironmentHandler<?> environmentHandler;

    private AnimationTimer frameTimer;

    private double speed = 1;
    private boolean evenFrameNumber = false;

    private final ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> configuration = new SimpleObjectProperty<>();
    private final ObjectProperty<StateHandler<OptimizationState>> state = new SimpleObjectProperty<>();

    private final BooleanProperty restartedProperty = new SimpleBooleanProperty();
    private final BooleanProperty singleEntityMode = new SimpleBooleanProperty(false);
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

            this.canvas.setWidth(this.environmentHandler.getEnvironmentWidth());
            this.canvas.setHeight(this.environmentHandler.getEnvironmentHeight());
            this.restartedProperty.bind(this.environmentHandler.restartedProperty());
            this.environmentHandler.singleEntityModeProperty().bind(this.singleEntityMode);
        }
    }

    protected void resetEnvironment() {
        getGraphicsContext().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        this.environmentHandler = null;
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

        if (this.evenFrameNumber && this.speed - floored > 0) {
            environmentHandler.update();
        }
    }

    public void startEnvironment() {
        // initialization necessary if environment handler isn't initialized
        if (this.environmentHandler == null) {
            initializeController();
        }
        this.frameTimer.start();
    }

    @FXML
    public void pauseEnvironment() {
        if (this.environmentHandler == null) {
            return;
        }

        this.frameTimer.stop();
    }

    @FXML
    public void restartEnvironment() {
        pauseEnvironment();
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
}
