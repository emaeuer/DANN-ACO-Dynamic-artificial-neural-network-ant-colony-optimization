package de.emaeuer.environment.cartpole.elements.builder;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.bird.elements.FlappyBird;
import de.emaeuer.environment.bird.elements.builder.FlappyBirdBuilder;
import de.emaeuer.environment.cartpole.CartPoleEnvironment;
import de.emaeuer.environment.cartpole.configuration.CartPoleConfiguration;
import de.emaeuer.environment.cartpole.elements.Cart;
import de.emaeuer.environment.elements.builder.ElementBuilder;
import de.emaeuer.optimization.Solution;

public class CartPoleBuilder extends ElementBuilder<Cart, CartPoleBuilder> {

    public CartPoleBuilder() {
        super();
        getElement().setMass(1);
        borderColor(0, 0, 0);
    }

    public CartPoleBuilder configuration(ConfigurationHandler<CartPoleConfiguration> configuration) {
        getElement().configure(configuration);
        return getThis();
    }

    @Override
    protected Cart getElementImplementation() {
        return new Cart();
    }

    @Override
    protected CartPoleBuilder getThis() {
        return this;
    }

    public CartPoleBuilder environment(CartPoleEnvironment environment) {
        getElement().setEnvironment(environment);
        return getThis();
    }

    public CartPoleBuilder solution(Solution solution) {
        getElement().setSolution(solution);
        return getThis();
    }
}
