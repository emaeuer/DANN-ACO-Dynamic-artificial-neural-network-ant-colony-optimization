package de.emaeuer.gui.controller.environment.handler;

import de.emaeuer.environment.cartpole.CartPoleEnvironment;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.gui.controller.util.ShapeDrawer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.GraphicsContext;

public class CartPoleHandler implements EnvironmentHandler {

    private final CartPoleEnvironment environment;

    private final GraphicsContext graphics;

    private final BooleanProperty restartedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty singleEntityMode = new SimpleBooleanProperty();

    public CartPoleHandler(CartPoleEnvironment environment, GraphicsContext graphics) {
        this.environment = environment;
        this.graphics = graphics;
    }

    @Override
    public void drawContent() {
        if (this.environment.areAllCartsDead()) {
            this.environment.restart();
            this.restartedProperty.set(true);
        } else {
            this.restartedProperty.set(false);
        }

        this.graphics.clearRect(0, 0, this.graphics.getCanvas().getWidth(), this.graphics.getCanvas().getHeight());
        this.graphics.strokeLine(0, 600, this.graphics.getCanvas().getWidth(), 600);
        this.graphics.strokeLine(this.graphics.getCanvas().getWidth() / 2, 0, this.graphics.getCanvas().getWidth() / 2, 600);
        this.environment.getParticles()
                .stream()
                .limit(singleEntityMode.get() ? 1 : Long.MAX_VALUE)
                .forEach(this::drawElement);
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
