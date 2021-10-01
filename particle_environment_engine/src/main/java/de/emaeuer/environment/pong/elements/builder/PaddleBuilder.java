package de.emaeuer.environment.pong.elements.builder;

import de.emaeuer.environment.elements.builder.ElementBuilder;
import de.emaeuer.environment.pong.elements.Paddle;

public class PaddleBuilder extends ElementBuilder<Paddle, PaddleBuilder> {

    @Override
    protected Paddle getElementImplementation() {
        return new Paddle();
    }

    @Override
    protected PaddleBuilder getThis() {
        return this;
    }

}
