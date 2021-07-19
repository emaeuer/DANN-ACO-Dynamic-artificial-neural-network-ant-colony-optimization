package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.balance.twodim.configuration.TwoDimensionalCartPoleConfiguration;
import de.emaeuer.environment.bird.configuration.FlappyBirdConfiguration;
import de.emaeuer.environment.balance.onedim.configuration.CartPoleConfiguration;
import de.emaeuer.environment.xor.XORConfiguration;

public class EnvironmentConfigurationFactory {
    
    private EnvironmentConfigurationFactory() {}
    
    public static ConfigurationHandler<?> createEnvironmentConfiguration(EnvironmentImplementations name) {
        return switch (name) {
            case FLAPPY_BIRD -> new ConfigurationHandler<>(FlappyBirdConfiguration.class, "FLAPPY_BIRD");
            case ONE_DIMENSIONAL_CART_POLE -> new ConfigurationHandler<>(CartPoleConfiguration.class, "ONE_D_CART_POLE");
            case XOR -> new ConfigurationHandler<>(XORConfiguration.class, "XOR");
            case TWO_DIMENSIONAL_CART_POLE -> new ConfigurationHandler<>(TwoDimensionalCartPoleConfiguration.class, "TWO_D_CART_POLE");
        };
    }
    
}
