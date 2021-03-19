package de.emaeuer.gui.controller.environment.handler;

import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.gui.controller.environment.handler.EnvironmentHandler;
import de.emaeuer.gui.controller.util.ShapeDrawer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class FlappyBirdHandler implements EnvironmentHandler {

    private final FlappyBirdEnvironment environment;

    private final GraphicsContext graphics;

    private final BooleanProperty restartedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty singleEntityMode = new SimpleBooleanProperty();

    public FlappyBirdHandler(FlappyBirdEnvironment environment, GraphicsContext graphics) {
        this.environment = environment;
        this.graphics = graphics;
    }

    @Override
    public void drawContent() {
        if (this.environment.areAllBirdsDead()) {
            this.environment.restart();
            this.restartedProperty.set(true);
        } else {
            this.restartedProperty.set(false);
        }

        this.graphics.clearRect(0, 0, this.graphics.getCanvas().getWidth(), this.graphics.getCanvas().getHeight());
        this.environment.getParticles()
                .stream()
                .limit(singleEntityMode.get() ? 1 : Long.MAX_VALUE)
                .forEach(this::drawElement);
        this.environment.getPipes().forEach(this::drawElement);
    }

    private void drawElement(AbstractElement element) {
        ShapeDrawer.drawElement(element, this.graphics);
    }

    @Override
    public void update() {
        this.environment.update();
    }

    @Override
    public double getEnvironmentWidth() {
        return this.environment.getWidth();
    }

    @Override
    public double getEnvironmentHeight() {
        return this.environment.getHeight();
    }

    @Override
    public BooleanProperty restartedProperty() {
        return this.restartedProperty;
    }

    @Override
    public BooleanProperty singleEntityModeProperty() {
        return this.singleEntityMode;
    }

}
