package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.balance.onedim.configuration.CartPoleGeneralizationConfiguration;
import de.emaeuer.environment.balance.twodim.configuration.TwoDimensionalCartPoleGeneralizationConfiguration;
import de.emaeuer.environment.bird.configuration.FlappyBirdGeneralizationConfiguration;
import de.emaeuer.environment.xor.XORGeneralizationConfiguration;

public class GeneralizationConfigurationFactory {

    private GeneralizationConfigurationFactory() {
    }

    public static ConfigurationHandler<?> createConfiguration(EnvironmentImplementations name) {
        return switch (name) {
            case FLAPPY_BIRD -> new ConfigurationHandler<>(FlappyBirdGeneralizationConfiguration.class, "FLAPPY_BIRD_GENERALIZATION");
            case XOR -> new ConfigurationHandler<>(XORGeneralizationConfiguration.class, "XOR_GENERALIZATION");
            case ONE_DIMENSIONAL_CART_POLE -> new ConfigurationHandler<>(CartPoleGeneralizationConfiguration.class, "ONE_D_CART_POLE_GENERALIZATION");
            case TWO_DIMENSIONAL_CART_POLE -> new ConfigurationHandler<>(TwoDimensionalCartPoleGeneralizationConfiguration.class, "TWO_D_CART_POLE_GENERALIZATION");
        };
    }
}
