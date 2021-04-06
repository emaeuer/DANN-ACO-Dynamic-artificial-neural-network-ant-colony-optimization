package de.emaeuer.environment.bird.elements.builder;

import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.bird.elements.FlappyBird;
import de.emaeuer.environment.elements.builder.ElementBuilder;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;

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

    public FlappyBirdBuilder controller(AgentController controller) {
        getElement().setController(controller);
        return getThis();
    }

}
