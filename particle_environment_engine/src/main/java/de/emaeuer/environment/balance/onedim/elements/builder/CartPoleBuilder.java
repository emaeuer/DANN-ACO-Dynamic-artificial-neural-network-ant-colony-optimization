package de.emaeuer.environment.balance.onedim.elements.builder;

import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.balance.onedim.CartPoleEnvironment;
import de.emaeuer.environment.balance.onedim.GeneralCartPoleData;
import de.emaeuer.environment.balance.onedim.elements.Cart;
import de.emaeuer.environment.elements.builder.ElementBuilder;

public class CartPoleBuilder extends ElementBuilder<Cart, CartPoleBuilder> {

    public CartPoleBuilder() {
        super();
        getElement().setMass(1);
    }

    public CartPoleBuilder configuration(GeneralCartPoleData cartPoleData) {
        getElement().configure(cartPoleData);
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

    public CartPoleBuilder controller(AgentController controller) {
        getElement().setController(controller);
        return getThis();
    }
}
