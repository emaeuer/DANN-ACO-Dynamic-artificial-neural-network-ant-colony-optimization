package de.emaeuer.gui.controller.environment.handler;

import de.emaeuer.environment.cartpole.CartPoleEnvironment;
import javafx.scene.canvas.GraphicsContext;

public class CartPoleHandler extends EnvironmentHandler<CartPoleEnvironment> {

    public CartPoleHandler(CartPoleEnvironment environment, GraphicsContext graphics) {
        super(environment, graphics);
    }

    @Override
    public void drawContent() {
        super.drawContent();
        getGraphics().strokeLine(0, 600, getGraphics().getCanvas().getWidth(), 600);
        getGraphics().strokeLine(getGraphics().getCanvas().getWidth() / 2, 0, getGraphics().getCanvas().getWidth() / 2, 600);
    }

}
