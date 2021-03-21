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

public class FlappyBirdHandler extends EnvironmentHandler<FlappyBirdEnvironment> {

    public FlappyBirdHandler(FlappyBirdEnvironment environment, GraphicsContext graphics) {
        super(environment, graphics);
    }

    @Override
    public void drawContent() {
        super.drawContent();
        getEnvironment().getPipes().forEach(this::drawElement);
    }

}
