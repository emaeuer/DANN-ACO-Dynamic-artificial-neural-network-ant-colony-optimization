package de.emaeuer.environment.factory;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.balance.CartPoleEnvironment;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.configuration.EnvironmentImplementations;
import de.emaeuer.environment.xor.XOREnvironment;

public class EnvironmentFactory {

    private EnvironmentFactory() {}

    public static AbstractEnvironment createEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        EnvironmentImplementations implementation = EnvironmentImplementations.valueOf(configuration.getValue(EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION_NAME, String.class));

        return switch (implementation) {
            case CART_POLE -> new CartPoleEnvironment(configuration);
            case FLAPPY_BIRD -> new FlappyBirdEnvironment(configuration);
            case XOR -> new XOREnvironment(configuration);
        };
    }

}
