package de.emaeuer.environment.balance;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.GeneralizationHandler;
import de.emaeuer.environment.balance.configuration.CartPoleConfiguration;
import de.emaeuer.environment.balance.configuration.CartPoleGeneralizationConfiguration;
import de.emaeuer.environment.balance.elements.Cart;
import de.emaeuer.environment.balance.elements.builder.CartPoleBuilder;
import de.emaeuer.environment.bird.configuration.FlappyBirdConfiguration;
import de.emaeuer.environment.bird.configuration.FlappyBirdGeneralizationConfiguration;
import de.emaeuer.environment.bird.elements.FlappyBird;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class CartPoleEnvironment extends AbstractEnvironment<CartPoleGeneralizationConfiguration> {

    private static final double CART_Y = 600;

    private boolean areAllCartsDead = false;

    private GeneralCartPoleData cartPoleData;

    private final ConfigurationHandler<CartPoleGeneralizationConfiguration> generalizationConfig;

    public CartPoleEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);

        if (configuration.getValue(EnvironmentConfiguration.TEST_GENERALIZATION, Boolean.class)) {
            this.generalizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(configuration, CartPoleGeneralizationConfiguration.class, EnvironmentConfiguration.GENERALIZATION_IMPLEMENTATION);
        } else {
            this.generalizationConfig = null;
        }
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
                .forEach(getAgentsToDraw()::add);
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
        getAgentsToDraw().stream()
                .filter(Cart.class::isInstance)
                .map(Cart.class::cast)
                .peek(Cart::incrementTimeStep)
                .peek(this::checkReachedMaxStepNumber)
                .filter(Cart::isDead)
                .forEach(deadCarts::add);

        getAgentsToDraw().removeAll(deadCarts);
        this.areAllCartsDead = getAgentsToDraw().isEmpty();
    }

    @Override
    protected GeneralizationHandler<CartPoleGeneralizationConfiguration> getNewGeneralizationHandler() {
        return new GeneralizationHandler<>(this.generalizationConfig, CartPoleGeneralizationConfiguration.getKeysForGeneralization());
    }

    @Override
    public void nextGeneralizationIteration() {
        super.nextGeneralizationIteration();

        if (finishedGeneralization()) {
            return;
        }

        while (!this.getGeneralizationHandler().reachedEnd()) {
            for (AgentController agent : getAgents()) {
                AgentController copy = agent.copy();
                getOriginControllers().put(copy, agent);

                Cart cart = createNextCart(copy);
                getAgentsToDraw().add(cart);
            }
            getGeneralizationHandler().next();
        }
    }

    private Cart createNextCart(AgentController controller) {
        Cart cart = buildCartPole(controller);

        // numbers taken from Stanley's neat generalization test
        cart.setCartPosition(getGeneralizationHandler().getNextValue(CartPoleGeneralizationConfiguration.POSITION_START_VALUE) * 4.32 - 2.16);
        cart.setCartVelocity(getGeneralizationHandler().getNextValue(CartPoleGeneralizationConfiguration.CART_VELOCITY_START_VALUE) * 2.70 - 1.35);
        cart.setPoleOneAngle(getGeneralizationHandler().getNextValue(CartPoleGeneralizationConfiguration.ANGLE_START_VALUE) * 0.12566304 - 0.06283152);
        cart.setPoleOneVelocity(getGeneralizationHandler().getNextValue(CartPoleGeneralizationConfiguration.ANGLE_VELOCITY_START_VALUE) * 0.30019504 - 0.15009752);

        return cart;
    }

    private void checkReachedMaxStepNumber(Cart cart) {
        if (!isTestingGeneralization() && cart.getStep() >= getMaxStepNumber()) {
            cart.setDead(true);
            setControllerFinishedWithoutDying(true);
        } else if (isTestingGeneralization() && cart.getStep() >= getMaxGeneralizationStepNumber()) {
            System.out.println(cart.getStep());
            cart.setDead(true);
            setControllerFinishedWithoutDying(true);

            AgentController origin = getOriginControllers().get(cart.getController());
            origin.setGeneralizationCapability(origin.getGeneralizationCapability() + (1.0 / getGeneralizationHandler().getNumberOfGeneralizationIterations()));
            setCurrentGeneralizationCapability(Math.max(getCurrentGeneralizationProgress(), origin.getGeneralizationCapability()));
        }
    }

    @Override
    public boolean environmentFinished() {
        return areAllCartsDead;
    }
}
