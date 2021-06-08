package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.bird.configuration.FlappyBirdConfiguration;
import de.emaeuer.environment.balance.configuration.CartPoleConfiguration;

public class EnvironmentConfigurationFactory {
    
    private EnvironmentConfigurationFactory() {}
    
    public static ConfigurationHandler<?> createEnvironmentConfiguration(EnvironmentImplementations name) {
        return switch (name) {
            case FLAPPY_BIRD -> new ConfigurationHandler<>(FlappyBirdConfiguration.class, "FLAPPY_BIRD");
            case CART_POLE -> new ConfigurationHandler<>(CartPoleConfiguration.class, "CART_POLE");
            case XOR -> null;
        };
    }
    
}
