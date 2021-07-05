package de.emaeuer.environment.balance;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.balance.configuration.CartPoleConfiguration;
import de.emaeuer.environment.balance.elements.Cart;
import de.emaeuer.environment.balance.elements.builder.CartPoleBuilder;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class CartPoleEnvironment extends AbstractEnvironment {

    private static final double CART_Y = 600;

    private boolean areAllCartsDead = false;

    private GeneralCartPoleData cartPoleData;

    public CartPoleEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);
    }

    @Override
    protected void initialize(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super.initialize(configuration);
        ConfigurationHandler<CartPoleConfiguration> cartConfiguration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, CartPoleConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);
        this.cartPoleData = new GeneralCartPoleData(cartConfiguration);
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
                .configuration(this.cartPoleData)
                .size(new Vector2D(100, 50))
                .environment(this)
                .setStartPosition(getWidth() / 2, CART_Y)
                .maxVelocity(Double.MAX_VALUE);

        return builder.build();
    }

    @Override
    public void restart() {
        super.restart();
        this.areAllCartsDead = false;
    }

    @Override
    public void step() {
        super.step();

        // increment scores and check if at least one bird lives
        List<Cart> deadCarts = new ArrayList<>();
        getAgents().stream()
                .filter(Cart.class::isInstance)
                .map(Cart.class::cast)
                .peek(Cart::incrementTimeStep)
                .peek(this::checkReachedMaxStepNumber)
                .filter(Cart::isDead)
                .forEach(deadCarts::add);

        getAgents().removeAll(deadCarts);
        this.areAllCartsDead = getAgents().isEmpty();
    }

    private void checkReachedMaxStepNumber(Cart cart) {
        if (cart.getStep() >= getMaxStepNumber()) {
            cart.setDead(true);
        }
    }

    private void checkReachedMaximumFitness(Cart cart) {
        if (cart.getFitness() >= getMaxFitnessScore()) {
            cart.setDead(true);
        }
    }

    @Override
    public boolean allAgentsFinished() {
        return areAllCartsDead;
    }
}
