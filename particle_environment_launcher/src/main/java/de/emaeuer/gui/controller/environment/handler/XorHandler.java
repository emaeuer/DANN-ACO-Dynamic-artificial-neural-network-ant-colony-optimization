package de.emaeuer.gui.controller.environment.handler;

import de.emaeuer.environment.xor.XorEnvironment;
import javafx.scene.canvas.GraphicsContext;

public class XorHandler extends EnvironmentHandler<XorEnvironment> {
    protected XorHandler(XorEnvironment environment, GraphicsContext graphics) {
        super(environment, graphics);
    }
}
