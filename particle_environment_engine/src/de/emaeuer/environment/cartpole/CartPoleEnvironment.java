package de.emaeuer.environment.cartpole;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.cartpole.configuration.CartPoleConfiguration;
import de.emaeuer.environment.cartpole.elements.Cart;
import de.emaeuer.environment.cartpole.elements.builder.CartPoleBuilder;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.math.Vector2D;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;

import java.util.ArrayList;
import java.util.List;

public class CartPoleEnvironment extends AbstractEnvironment {

    private static final double CART_Y = 600;

    private boolean areAllCartsDead = false;
    private Cart bestCart;

    private ConfigurationHandler<CartPoleConfiguration> configuration;

    public CartPoleEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration, StateHandler<OptimizationState> state) {
        super(null, configuration, state);
    }

    @Override
    protected void initialize(ConfigurationHandler<EnvironmentConfiguration> configuration, StateHandler<OptimizationState> state) {
        super.initialize(configuration, state);
        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, CartPoleConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);
    }

    @Override
    protected void initializeParticles() {
        getOptimization().nextIteration()
                .stream()
                .map(this::buildCartPole)
                .forEach(getParticles()::add);

        if (getOptimization().getCurrentlyBestSolution() != null) {
            this.bestCart = buildCartPole(getOptimization().getCurrentlyBestSolution());
            getParticles().add(this.bestCart);
        }
    }

    private Cart buildCartPole(Solution brain) {
        CartPoleBuilder builder = new CartPoleBuilder()
                .solution(brain)
                .configuration(this.configuration)
                .size(new Vector2D(100, 50))
                .environment(this)
                .setStartPosition(getWidth() / 2, CART_Y)
                .maxVelocity(Double.MAX_VALUE);

        return builder.build();
    }

    @Override
    public void restart() {
        getOptimization().update();

        initializeParticles();
        this.areAllCartsDead = false;
    }

    @Override
    public void update() {
        super.update();

        if (isRestartNecessary()) {
            return;
        }

        // increment scores and check if at least one bird lives (ignore this.bestParticle)
        List<Cart> deadCarts = new ArrayList<>();
        getParticles().stream()
                .filter(p -> p != this.bestCart)
                .filter(Cart.class::isInstance)
                .map(Cart.class::cast)
                .peek(Cart::incrementScore)
                .peek(this::checkReachedMaximumFitness)
                .filter(Cart::isDead)
                .forEach(deadCarts::add);

        if (this.bestCart != null && this.bestCart.isDead()) {
            getParticles().remove(this.bestCart);
        }

        // terminate iteration if only this.bestParticle remains
        if (getParticles().size() == 1 && getParticles().get(0) == this.bestCart) {
            getParticles().clear();
        }

        getParticles().removeAll(deadCarts);
        this.areAllCartsDead = getParticles().isEmpty();
    }

    private void checkReachedMaximumFitness(Cart cart) {
        if (cart.getScore() >= getMaxFitnessScore()) {
            cart.setDead(true);
        }
    }

    @Override
    public boolean isRestartNecessary() {
        return areAllCartsDead;
    }
}
