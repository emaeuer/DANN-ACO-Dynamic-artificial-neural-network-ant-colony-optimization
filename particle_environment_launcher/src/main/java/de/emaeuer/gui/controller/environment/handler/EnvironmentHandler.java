package de.emaeuer.gui.controller.environment.handler;

import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.cartpole.CartPoleEnvironment;
import de.emaeuer.environment.elements.AbstractElement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.scene.canvas.GraphicsContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface EnvironmentHandler {

    final static Logger LOG = LogManager.getLogger(EnvironmentHandler.class);

    static <S extends AbstractEnvironment> EnvironmentHandler createEnvironmentHandler(S environment, GraphicsContext context) {
        if (environment instanceof FlappyBirdEnvironment flappyEnvironment) {
            return new FlappyBirdHandler(flappyEnvironment, context);
        } else if (environment instanceof CartPoleEnvironment cartEnvironment) {
            return new CartPoleHandler(cartEnvironment, context);
        }

        LOG.log(Level.WARN, "Failed to create environment of type {}. Reason: not implemented", environment.getClass().getSimpleName());
        return null;
    }

    void drawContent();

    void update();

    double getEnvironmentWidth();

    double getEnvironmentHeight();

    BooleanProperty restartedProperty();

    BooleanProperty singleEntityModeProperty();

}
