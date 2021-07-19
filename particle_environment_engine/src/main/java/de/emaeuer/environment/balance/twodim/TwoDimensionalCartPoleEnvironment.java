package de.emaeuer.environment.balance.twodim;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.GeneralizationHandler;
import de.emaeuer.environment.balance.twodim.configuration.TwoDimensionalCartPoleConfiguration;
import de.emaeuer.environment.balance.twodim.configuration.TwoDimensionalCartPoleGeneralizationConfiguration;
import de.emaeuer.environment.balance.twodim.element.TwoDimensionalCart;
import de.emaeuer.environment.balance.twodim.element.builder.TwoDimensionalCartPoleBuilder;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class TwoDimensionalCartPoleEnvironment extends AbstractEnvironment<TwoDimensionalCartPoleGeneralizationConfiguration> {

    private boolean areAllCartsDead = false;

    private final GeneralTwoDimensionalCartPoleData cartPoleData;

    public TwoDimensionalCartPoleEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);

        ConfigurationHandler<TwoDimensionalCartPoleConfiguration> configuration1 = ConfigurationHelper.extractEmbeddedConfiguration(configuration, TwoDimensionalCartPoleConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);
        this.cartPoleData = new GeneralTwoDimensionalCartPoleData(configuration1);
    }

    @Override
    protected void initializeParticles(List<AgentController> controllers) {
        controllers.stream()
                .map(this::buildCartPole)
                .forEach(getAgentsToDraw()::add);
    }

    private TwoDimensionalCart buildCartPole(AgentController controller) {
        TwoDimensionalCartPoleBuilder builder = new TwoDimensionalCartPoleBuilder()
                .controller(controller)
                .environment(this)
                .configuration(this.cartPoleData)
                .size(new Vector2D(75, 75))
                .setStartPosition(getWidth() / 2, getHeight() / 2)
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
        List<TwoDimensionalCart> deadCarts = new ArrayList<>();
        getAgentsToDraw().stream()
                .filter(TwoDimensionalCart.class::isInstance)
                .map(TwoDimensionalCart.class::cast)
                .peek(TwoDimensionalCart::incrementTimeStep)
                .peek(this::checkReachedMaxStepNumber)
                .filter(TwoDimensionalCart::isDead)
                .peek(c -> c.getController().setScore(c.getController().getScore() / getMaxStepNumber()))
                .forEach(deadCarts::add);

        getAgentsToDraw().removeAll(deadCarts);
        this.areAllCartsDead = getAgentsToDraw().isEmpty();
    }


    @Override
    protected GeneralizationHandler<TwoDimensionalCartPoleGeneralizationConfiguration> getNewGeneralizationHandler() {
        return null;
    }

    private void checkReachedMaxStepNumber(TwoDimensionalCart cart) {
        if (cart.getStep() >= getMaxStepNumber()) {
            cart.setDead(true);
            setControllerFinishedWithoutDying(true);
        }
    }

    @Override
    public boolean environmentFinished() {
        return areAllCartsDead;
    }
}
