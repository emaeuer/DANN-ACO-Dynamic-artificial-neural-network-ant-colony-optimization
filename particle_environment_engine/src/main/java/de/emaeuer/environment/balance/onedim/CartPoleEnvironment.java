package de.emaeuer.environment.balance.onedim;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.GeneralizationHandler;
import de.emaeuer.environment.balance.onedim.configuration.CartPoleConfiguration;
import de.emaeuer.environment.balance.onedim.configuration.CartPoleGeneralizationConfiguration;
import de.emaeuer.environment.balance.onedim.elements.Cart;
import de.emaeuer.environment.balance.onedim.elements.builder.CartPoleBuilder;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class CartPoleEnvironment extends AbstractEnvironment<CartPoleGeneralizationConfiguration> {

    private enum Phases {
        NORMAL,
        GENERALIZATION_LONG_TEST,
        GENERALIZATION_VARIATION_TEST
    }

    private static final double CART_Y = 600;

    private boolean areAllCartsDead = false;

    private final GeneralCartPoleData cartPoleData;

    private final ConfigurationHandler<CartPoleConfiguration> configuration;
    private final ConfigurationHandler<CartPoleGeneralizationConfiguration> generalizationConfig;

    private Phases currentPhase = Phases.NORMAL;

    private boolean currentPhaseSuccess = false;

    public CartPoleEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, CartPoleConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);
        this.cartPoleData = new GeneralCartPoleData(this.configuration);

        if (configuration.getValue(EnvironmentConfiguration.TEST_GENERALIZATION, Boolean.class)) {
            this.generalizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(configuration, CartPoleGeneralizationConfiguration.class, EnvironmentConfiguration.GENERALIZATION_IMPLEMENTATION);
        } else {
            this.generalizationConfig = null;
        }
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
        this.currentPhase = Phases.NORMAL;
        this.currentPhaseSuccess = false;
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
                .peek(Cart::calculateFitness)
                .peek(this::normalizeFitness)
                .forEach(deadCarts::add);

        getAgentsToDraw().removeAll(deadCarts);
        this.areAllCartsDead = getAgentsToDraw().isEmpty();
    }


    @Override
    protected GeneralizationHandler<CartPoleGeneralizationConfiguration> getNewGeneralizationHandler() {
        return new GeneralizationHandler<>(this.generalizationConfig, CartPoleGeneralizationConfiguration.getKeysForGeneralization());
    }

    @Override
    public void testGeneralization() {
        super.testGeneralization();
    }

    @Override
    public void nextGeneralizationIteration() {
        super.nextGeneralizationIteration();

        if (this.currentPhase == Phases.GENERALIZATION_LONG_TEST && !this.currentPhaseSuccess) {
            setFinishedGeneralization(true);
            setTestingGeneralization(false);
        }

        if (finishedGeneralization()) {
            return;
        }

        if (this.currentPhase == Phases.NORMAL) {
            initLongTest();
        } else if (this.currentPhase == Phases.GENERALIZATION_LONG_TEST) {
            initVariationTest();
        }
    }

    private void initLongTest() {
        this.currentPhase = Phases.GENERALIZATION_LONG_TEST;
        this.currentPhaseSuccess = false;
        getAgents().stream()
                .map(AgentController::copy)
                .map(this::buildCartPole)
                .forEach(cart -> getAgentsToDraw().add(cart));
    }

    private void initVariationTest() {
        this.currentPhase = Phases.GENERALIZATION_VARIATION_TEST;
        this.currentPhaseSuccess = false;
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
        if (this.currentPhase == Phases.NORMAL && cart.getStep() >= getMaxStepNumber()) {
            cart.setDead(true);
            setControllerFinishedWithoutDying(true);
            this.currentPhaseSuccess = true;
        } else if (this.currentPhase == Phases.GENERALIZATION_LONG_TEST && cart.getStep() >= 100000) { // TODO extract long test step number to configuration
            cart.setDead(true);
            setControllerFinishedWithoutDying(true);
            this.currentPhaseSuccess = true;
        } else if (this.currentPhase == Phases.GENERALIZATION_VARIATION_TEST && cart.getStep() >= getMaxGeneralizationStepNumber()) {
            cart.setDead(true);
            setControllerFinishedWithoutDying(true);
            this.currentPhaseSuccess = true;

            AgentController origin = getOriginControllers().get(cart.getController());
            origin.setGeneralizationCapability(origin.getGeneralizationCapability() + (1.0 / getGeneralizationHandler().getNumberOfGeneralizationIterations()));
            setCurrentGeneralizationCapability(Math.max(getCurrentGeneralizationProgress(), origin.getGeneralizationCapability()));
        }
    }

    private void normalizeFitness(Cart cart) {
        if (this.configuration.getValue(CartPoleConfiguration.PENALIZE_OSCILLATION, Boolean.class)) {
            // 10000 because of scaling in oscillation fitness function
            cart.getController().setScore(cart.getController().getScore() / getMaxStepNumber() * 10000);
        } else {
            cart.getController().setScore(cart.getController().getScore() / getMaxStepNumber());
        }
    }

    @Override
    public boolean environmentFinished() {
        return areAllCartsDead;
    }
}
