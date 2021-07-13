package de.emaeuer.environment.bird.elements.builder;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.bird.configuration.FlappyBirdConfiguration;
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

    public FlappyBirdBuilder applyConfiguration(ConfigurationHandler<FlappyBirdConfiguration> configuration) {
        int inputPattern = 0;

        if (configuration.getValue(FlappyBirdConfiguration.HEIGHT_INPUT, Boolean.class)) {
            inputPattern += 1;
        }

        if (configuration.getValue(FlappyBirdConfiguration.VELOCITY_INPUT, Boolean.class)) {
            inputPattern += 2;
        }

        if (configuration.getValue(FlappyBirdConfiguration.GAP_INPUT, Boolean.class)) {
            inputPattern += 4;
        }

        if (configuration.getValue(FlappyBirdConfiguration.DISTANCE_INPUT, Boolean.class)) {
            inputPattern += 8;
        }


        getElement().setInputPattern(inputPattern);
        return getThis();
    }
}
