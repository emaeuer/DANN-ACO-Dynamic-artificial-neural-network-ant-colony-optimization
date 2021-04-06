package de.emaeuer.environment.cartpole.elements;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.cartpole.CartPoleEnvironment;
import de.emaeuer.environment.cartpole.configuration.CartPoleConfiguration;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.CartShape;
import de.emaeuer.environment.elements.shape.ShapeEntity;

import java.util.List;

import static de.emaeuer.environment.cartpole.configuration.CartPoleConfiguration.*;

public class Cart extends AbstractElement {

    private CartPoleEnvironment environment;

    private boolean dead = false;

    private double score = 0;

    private double theta = 0;
    private double thetaDot = 0;
    private double xDot = 0;

    private double gravity;
    private double massOfPole;
    private double poleLength;
    private double steeringForceMagnitude;
    private double tau;

    private double thetaThreshold;
    private double xThreshold;

    private AgentController controller;

    public Cart() {
        super(new CartShape());
    }

    @Override
    public void step() {
        if (isDead() || this.environment == null) {
            return;
        }

        // calculate the data of the agent
        double xInput = (getPosition().getX() - this.environment.getWidth() / 2 + getSize().getX() / 2) / this.xThreshold;
        double thetaInput = this.theta / this.thetaThreshold;

        // get the action of the agent controller
        boolean correctLeft = this.controller.getAction(new double[]{xInput, thetaInput}) == 1;
        updateCartPoleState(correctLeft);

        // check solution died
        checkDied();
    }



    private void updateCartPoleState(boolean correctToLeft) {
        double force = this.steeringForceMagnitude * (correctToLeft ? -1 : 1);

        // calculate acceleration (change of this step) (more information https://coneural.org/florian/papers/05_cart_pole.pdf)
        double temp = (force + this.massOfPole * this.poleLength / 2 * Math.pow(this.thetaDot, 2) * Math.sin(this.theta)) / getMass();
        double thetaAcceleration = (this.gravity * Math.sin(this.theta) - Math.cos(this.theta) * temp) / (this.poleLength / 2 * (4.0 / 3 - this.massOfPole * this.poleLength / 2 * Math.pow(Math.cos(this.theta), 2) / getMass()));
        double xAcceleration = temp - this.massOfPole * this.poleLength / 2 * thetaAcceleration * Math.cos(this.theta) / getMass();

        // euler integration
        getPosition().setX(getPosition().getX() + this.tau * this.xDot);
        this.xDot = xDot + this.tau * xAcceleration;
        this.theta = this.theta + this.tau * this.thetaDot;
        thetaDot = this.thetaDot + this.tau * thetaAcceleration;
    }

    private void checkDied() {
        this.dead |= Math.abs(getPosition().getX() - this.environment.getWidth() / 2) > this.xThreshold;
        this.dead |= Math.abs(this.theta) > this.thetaThreshold;
    }

    public void configure(ConfigurationHandler<CartPoleConfiguration> configuration) {
        this.gravity = configuration.getValue(GRAVITY, Double.class);
        this.massOfPole = configuration.getValue(POLE_MASS, Double.class);
        setMass(configuration.getValue(CART_MASS, Double.class));
        this.poleLength = configuration.getValue(POLE_LENGTH, Double.class);
        this.steeringForceMagnitude = configuration.getValue(STEERING_FORCE, Double.class);
        this.tau = configuration.getValue(TAU, Double.class);
        this.thetaThreshold = configuration.getValue(THETA_THRESHOLD, Double.class);
        this.xThreshold = configuration.getValue(X_THRESHOLD, Double.class);
    }

    public void setController(AgentController controller) {
        this.controller = controller;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public double getScore() {
        return score;
    }

    public void incrementScore() {
        this.score += 1;
        this.controller.setScore(this.score);
    }

    public void setEnvironment(CartPoleEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((CartShape) this.getShape()).getShapesForElement(this);
    }

    public double getTheta() {
        return theta;
    }

    public double getPoleLength() {
        return poleLength;
    }
}
