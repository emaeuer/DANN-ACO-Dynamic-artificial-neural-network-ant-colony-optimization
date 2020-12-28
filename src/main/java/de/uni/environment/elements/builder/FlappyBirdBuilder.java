package de.uni.environment.elements.builder;

import de.uni.optimization.Solution;
import de.uni.environment.elements.FlappyBird;
import de.uni.environment.impl.FlappyBirdEnvironment;

public class FlappyBirdBuilder extends ElementBuilder<FlappyBird, FlappyBirdBuilder> {

    public FlappyBirdBuilder() {
        super();
        getElement().setRadius(20);
        getElement().setMass(20);
        borderColor(0, 0, 0);
    }

    @Override
    protected FlappyBird getElementImplementation() {
        return new FlappyBird();
    }

    @Override
    protected FlappyBirdBuilder getThis() {
        return this;
    }

    public FlappyBirdBuilder radius(double radius) {
        getElement().setRadius(radius);
        return getThis();
    }

    public FlappyBirdBuilder environment(FlappyBirdEnvironment environment) {
        getElement().setEnvironment(environment);
        return getThis();
    }

    public FlappyBirdBuilder solution(Solution solution) {
        getElement().setSolution(solution);
        return getThis();
    }

}
