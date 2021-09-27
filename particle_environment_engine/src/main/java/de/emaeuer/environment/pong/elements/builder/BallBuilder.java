package de.emaeuer.environment.pong.elements.builder;

import de.emaeuer.environment.elements.builder.ElementBuilder;
import de.emaeuer.environment.pong.elements.Ball;

public class BallBuilder extends ElementBuilder<Ball, BallBuilder> {

    @Override
    protected Ball getElementImplementation() {
        return new Ball();
    }

    @Override
    protected BallBuilder getThis() {
        return this;
    }

    public BallBuilder startDirection(double degrees, double amplitude) {
        double radians = Math.toRadians(degrees);
        double xVelocity = amplitude * Math.cos(radians);
        double yVelocity = amplitude * Math.sin(radians);

        getElement().getVelocity().setX(xVelocity);
        getElement().getVelocity().setY(yVelocity);

        return this;
    }
}
