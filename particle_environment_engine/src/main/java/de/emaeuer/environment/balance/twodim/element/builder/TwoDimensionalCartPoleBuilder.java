package de.emaeuer.environment.balance.twodim.element.builder;

import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.balance.twodim.GeneralTwoDimensionalCartPoleData;
import de.emaeuer.environment.balance.twodim.TwoDimensionalCartPoleEnvironment;
import de.emaeuer.environment.balance.twodim.element.TwoDimensionalCart;
import de.emaeuer.environment.elements.builder.ElementBuilder;

public class TwoDimensionalCartPoleBuilder extends ElementBuilder<TwoDimensionalCart, TwoDimensionalCartPoleBuilder> {

    public TwoDimensionalCartPoleBuilder() {
        super();
        getElement().setMass(1);
        borderColor(0, 0, 0);
    }

    public TwoDimensionalCartPoleBuilder configuration(GeneralTwoDimensionalCartPoleData cartPoleData) {
        getElement().configure(cartPoleData);
        return getThis();
    }

    @Override
    protected TwoDimensionalCart getElementImplementation() {
        return new TwoDimensionalCart();
    }

    @Override
    protected TwoDimensionalCartPoleBuilder getThis() {
        return this;
    }

    public TwoDimensionalCartPoleBuilder environment(TwoDimensionalCartPoleEnvironment environment) {
        getElement().setEnvironment(environment);
        return getThis();
    }

    public TwoDimensionalCartPoleBuilder controller(AgentController controller) {
        getElement().setController(controller);
        return getThis();
    }
}
