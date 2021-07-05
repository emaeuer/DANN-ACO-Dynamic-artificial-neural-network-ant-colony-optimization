package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.balance.configuration.CartPoleConfiguration;
import de.emaeuer.environment.balance.configuration.CartPoleGeneralizationConfiguration;
import de.emaeuer.environment.bird.configuration.FlappyBirdConfiguration;
import de.emaeuer.environment.xor.XORConfiguration;

public class GeneralizationConfigurationFactory {

    private GeneralizationConfigurationFactory() {}

    public static ConfigurationHandler<?> createConfiguration(EnvironmentImplementations name) {
        return switch (name) {
            case FLAPPY_BIRD, XOR -> null;
            case CART_POLE -> new ConfigurationHandler<>(CartPoleGeneralizationConfiguration.class, "CART_POLE_GENERALIZATION");
        };
    }
}
