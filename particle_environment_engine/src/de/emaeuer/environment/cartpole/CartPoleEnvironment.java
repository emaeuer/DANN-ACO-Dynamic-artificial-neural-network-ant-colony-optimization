package de.emaeuer.environment.cartpole;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.cartpole.configuration.CartPoleConfiguration;
import de.emaeuer.environment.cartpole.elements.Cart;
import de.emaeuer.environment.cartpole.elements.builder.CartPoleBuilder;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class CartPoleEnvironment extends AbstractEnvironment {

    private static final double CART_Y = 600;

    private boolean areAllCartsDead = false;
    private Cart bestCart;

    private ConfigurationHandler<CartPoleConfiguration> configuration;

    public CartPoleEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);
    }

    @Override
    protected void initialize(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super.initialize(configuration);
        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, CartPoleConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);
    }

    @Override
    protected void initializeParticles(List<AgentController> controllers) {
        controllers.stream()
                .map(this::buildCartPole)
                .forEach(getAgents()::add);
    }

    private Cart buildCartPole(AgentController controller) {
        CartPoleBuilder builder = new CartPoleBuilder()
                .controller(controller)
                .configuration(this.configuration)
                .size(new Vector2D(100, 50))
                .environment(this)
                .setStartPosition(getWidth() / 2, CART_Y)
                .maxVelocity(Double.MAX_VALUE);

        return builder.build();
    }

    @Override
    public void restart() {
        this.areAllCartsDead = false;
    }

    @Override
    public void step() {
        super.step();

        // increment scores and check if at least one bird lives (ignore this.bestParticle)
        List<Cart> deadCarts = new ArrayList<>();
        getAgents().stream()
                .filter(p -> p != this.bestCart)
                .filter(Cart.class::isInstance)
                .map(Cart.class::cast)
                .peek(Cart::incrementScore)
                .peek(this::checkReachedMaximumFitness)
                .filter(Cart::isDead)
                .forEach(deadCarts::add);

        if (this.bestCart != null && this.bestCart.isDead()) {
            getAgents().remove(this.bestCart);
        }

        // terminate iteration if only this.bestParticle remains
        if (getAgents().size() == 1 && getAgents().get(0) == this.bestCart) {
            getAgents().clear();
        }

        getAgents().removeAll(deadCarts);
        this.areAllCartsDead = getAgents().isEmpty();
    }

    private void checkReachedMaximumFitness(Cart cart) {
        if (cart.getScore() >= getMaxFitnessScore()) {
            cart.setDead(true);
        }
    }

    @Override
    public boolean allAgentsFinished() {
        return areAllCartsDead;
    }
}
