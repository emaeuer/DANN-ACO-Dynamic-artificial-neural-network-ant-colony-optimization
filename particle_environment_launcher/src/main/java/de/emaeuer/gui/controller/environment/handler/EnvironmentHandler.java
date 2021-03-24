package de.emaeuer.gui.controller.environment.handler;

import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.cartpole.CartPoleEnvironment;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.gui.controller.util.ShapeDrawer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.GraphicsContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class EnvironmentHandler<T extends AbstractEnvironment> {

    private static final Logger LOG = LogManager.getLogger(EnvironmentHandler.class);

    public static <S extends AbstractEnvironment> EnvironmentHandler<S> createEnvironmentHandler(S environment, GraphicsContext context) {
        if (environment instanceof FlappyBirdEnvironment flappyEnvironment) {
            //noinspection unchecked no safe way to cast generics but is checked by instanceof
            return (EnvironmentHandler<S>) new FlappyBirdHandler(flappyEnvironment, context);
        } else if (environment instanceof CartPoleEnvironment cartEnvironment) {
            //noinspection unchecked no safe way to cast generics but is checked by instanceof
            return (EnvironmentHandler<S>) new CartPoleHandler(cartEnvironment, context);
        }

        LOG.log(Level.WARN, "Failed to create environment of type {}. Reason: not implemented", environment.getClass().getSimpleName());
        return null;
    }

    private final T environment;
    private final GraphicsContext graphics;

    private final BooleanProperty restartedProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty singleEntityMode = new SimpleBooleanProperty();
    private final BooleanProperty optimizationFinished = new SimpleBooleanProperty();

    private final DoubleProperty evaluationProgressProperty = new SimpleDoubleProperty(0);
    private final DoubleProperty fitnessProgressProperty = new SimpleDoubleProperty(0);

    protected EnvironmentHandler(T environment, GraphicsContext graphics) {
        this.environment = environment;
        this.graphics = graphics;
    }

    public void drawContent() {
        getGraphics().clearRect(0, 0, getGraphics().getCanvas().getWidth(), getGraphics().getCanvas().getHeight());
        getEnvironment().getParticles()
                .stream()
                .limit(singleEntityModeProperty().get() ? 1 : Long.MAX_VALUE)
                .forEach(this::drawElement);
    }

    protected void drawElement(AbstractElement element) {
        ShapeDrawer.drawElement(element, getGraphics());
    }

    public void update() {
        if (getEnvironment().isRestartNecessary()) {
            getEnvironment().restart();
            this.restartedProperty().set(true);
        } else {
            this.restartedProperty().set(false);
        }

        this.environment.update();

        this.fitnessProgressProperty.setValue(this.environment.getMaxFitness() / this.environment.getFitnessThreshold());
        this.evaluationProgressProperty.setValue((double) this.environment.getNumberOfEvaluations() / this.environment.getEvaluationThreshold());
        this.optimizationFinished.set(this.environment.isOptimizationFinished());
    }

    public double getEnvironmentWidth() {
        return getEnvironment().getWidth();
    }

    public double getEnvironmentHeight() {
        return getEnvironment().getHeight();
    }

    protected T getEnvironment() {
        return this.environment;
    }

    protected GraphicsContext getGraphics() {
        return this.graphics;
    }

    public BooleanProperty restartedProperty() {
        return this.restartedProperty;
    }

    public BooleanProperty singleEntityModeProperty() {
        return this.singleEntityMode;
    }

    public BooleanProperty finishedProperty() {
        return this.optimizationFinished;
    }

    public DoubleProperty evaluationProgressProperty() {
        return evaluationProgressProperty;
    }

    public DoubleProperty fitnessProgressProperty() {
        return fitnessProgressProperty;
    }
}
