package de.emaeuer.environment.balance.elements;

import com.google.common.collect.EvictingQueue;
import com.google.common.primitives.Doubles;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.balance.CartPoleEnvironment;
import de.emaeuer.environment.balance.GeneralCartPoleData;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.CartShape;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Cart extends AbstractElement {

    public static double TRACK_DISPLAY_SCALING = 100;

    private CartPoleEnvironment environment;

    private GeneralCartPoleData data;

    private boolean dead = false;

    private double fitness = 0;

    private int step = 0;

    Queue<Double> lastOscillations = EvictingQueue.create(100);

    private double cartPosition = 0;
    private double cartVelocity = 0;
    private double poleOneAngle = 0;
    private double poleOneVelocity = 0;
    private double poleTwoAngle = 0;
    private double poleTwoVelocity = 0;

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
        double[] input = createNetworkInput();

        // get the action of the agent controller
        double activation = this.controller.getAction(input)[0];
        activation = adjustActivation(activation, controller);

        if (data.twoPoles()) {
            updateCartTwoPolesState(activation);
        } else {
            updateCartOnePoleState(activation);
        }

        if (data.penalizeOscillation()) {
            this.lastOscillations.add(Math.abs(cartPosition) + Math.abs(cartVelocity) + Math.abs(poleOneAngle) + Math.abs(poleOneVelocity));
        }

        // refresh cart position
        refreshCartPosition();

        // check solution died
        checkDied();

        if (data.penalizeOscillation()) {
            if (this.step >= 100) {
                double oscillations = 0;
                while (!this.lastOscillations.isEmpty()) {
                    oscillations += this.lastOscillations.poll();
                }
                this.fitness = 0.1 * ((this.step + 1) / 1000.0) + 0.9 * (0.75 / oscillations);
            } else {
                this.fitness = 0.1 * ((this.step + 1) / 1000.0);
            }
        } else {
            this.fitness = this.step + 1;
        }
        this.controller.setScore(this.fitness);
    }

    private double adjustActivation(double activation, AgentController controller) {
        // 10 and -10 are arbitrary bounds in case of unlimited values
        double maxActivation = Math.min(controller.getMaxAction(), 10);
        double minActivation = Math.max(controller.getMinAction(), -10);
        double activationRange = maxActivation - minActivation;
        double middle = (maxActivation + minActivation) / 2;

        // adjust to [-1:1]
        return (2 * (activation - middle)) / activationRange;
    }

    private void refreshCartPosition() {
        double scaledXPosition = cartPosition * TRACK_DISPLAY_SCALING;
        getPosition().setX(scaledXPosition + (environment.getWidth() / 2));
    }

    private double[] createNetworkInput() {
        List<Double> inputs = new ArrayList<>();

        inputs.add(this.cartPosition / (data.trackLength() / 2));
        inputs.add(poleOneAngle / data.poleAngleThreshold());

        if (data.velocityInput()) {
            inputs.add(cartVelocity / 0.75);
            inputs.add(poleOneVelocity);
        }

        if (data.twoPoles()) {
            inputs.add(poleTwoAngle / data.poleAngleThreshold());
            if (data.velocityInput()) {
                inputs.add(poleTwoVelocity);
            }
        }

        return Doubles.toArray(inputs);
    }

    private void updateCartTwoPolesState(double activation) {
        double[] dydx = new double[6];
        for (int i = 0; i < 2; i++) {
            dydx[0] = cartVelocity;
            dydx[2] = poleOneVelocity;
            dydx[4] = poleTwoVelocity;

            double[] state = new double[]{cartPosition, cartVelocity, poleOneAngle, poleOneVelocity, poleTwoAngle, poleTwoVelocity};
            stepTwoPoles(activation, state, dydx);
            rk4TwoPoles(activation, state, dydx, state);

            this.cartPosition = state[0];
            this.cartVelocity = state[1];
            this.poleOneAngle = state[2];
            this.poleOneVelocity = state[3];
            this.poleTwoAngle = state[4];
            this.poleTwoVelocity = state[5];
        }
    }

    private void stepTwoPoles(double action, double[] st, double[] derivations) {
        double force = action * data.forceMagnitude();

        double poleOneCosTheta = Math.cos(st[2]);
        double poleOneSinTheta = Math.sin(st[2]);
        double poleTwoCosTheta = Math.cos(st[4]);
        double poleTwoSinTheta = Math.sin(st[4]);

        double ml_1 = data.poleOneLength() * data.poleOneMass();
        double ml_2 = data.poleTwoLength() * data.poleTwoMass();
        double temp_1 = data.pivotFriction() * st[3] / ml_1;
        double temp_2 = data.pivotFriction() * st[5] / ml_2;

        double fi_1 = (ml_1 * st[3] * st[3] * poleOneSinTheta)
                + (0.75 * data.poleOneMass() * poleOneCosTheta * (temp_1 + (data.gravity() * poleOneSinTheta)));

        double fi_2 = (ml_2 * st[5] * st[5] * poleTwoSinTheta)
                + (0.75 * data.poleTwoMass() * poleTwoCosTheta * (temp_2 + (data.gravity() * poleTwoSinTheta)));

        double mi_1 = data.poleOneMass() * (1 - (0.75 * poleOneCosTheta * poleOneCosTheta));
        double mi_2 = data.poleTwoMass() * (1 - (0.75 * poleTwoCosTheta * poleTwoCosTheta));

        derivations[1] = (force + fi_1 + fi_2) / (mi_1 + mi_2 + data.cartMass());
        derivations[3] = -0.75 * (derivations[1] * poleOneCosTheta + (data.gravity() * poleOneSinTheta) + temp_1) / data.poleOneLength();
        derivations[5] = -0.75 * (derivations[1] * poleTwoCosTheta + (data.gravity() * poleTwoSinTheta) + temp_2) / data.poleTwoLength();
    }

    private void rk4TwoPoles(double f, double[] y, double[] dydx, double[] yout) {
        int i;

        double hh, h6;
        double[] dym = new double[6];
        double[] dyt = new double[6];
        double[] yt = new double[6];

        hh = data.timeDelta() * 0.5;
        h6 = data.timeDelta() / 6.0;
        for (i = 0; i <= 5; i++)
            yt[i] = y[i] + hh * dydx[i];
        stepTwoPoles(f, yt, dyt);
        dyt[0] = yt[1];
        dyt[2] = yt[3];
        dyt[4] = yt[5];
        for (i = 0; i <= 5; i++)
            yt[i] = y[i] + hh * dyt[i];
        stepTwoPoles(f, yt, dym);
        dym[0] = yt[1];
        dym[2] = yt[3];
        dym[4] = yt[5];
        for (i = 0; i <= 5; i++) {
            yt[i] = y[i] + data.timeDelta() * dym[i];
            dym[i] += dyt[i];
        }
        stepTwoPoles(f, yt, dyt);
        dyt[0] = yt[1];
        dyt[2] = yt[3];
        dyt[4] = yt[5];
        for (i = 0; i <= 5; i++)
            yout[i] = y[i] + h6 * (dydx[i] + dyt[i] + 2.0 * dym[i]);
    }

    private void updateCartOnePoleState(double activation) {
        double[] dydx = new double[4];
        for (int i = 0; i < 2; i++) {
            dydx[0] = cartVelocity;
            dydx[2] = poleOneVelocity;

            double[] state = new double[]{cartPosition, cartVelocity, poleOneAngle, poleOneVelocity};
            stepOnePole(activation, state, dydx);
            rk4OnePole(activation, state, dydx, state);

            this.cartPosition = state[0];
            this.cartVelocity = state[1];
            this.poleOneAngle = state[2];
            this.poleOneVelocity = state[3];
        }
    }

    private void stepOnePole(double action, double[] st, double[] derivations) {
        double force = action * data.forceMagnitude();

        double poleOneCosTheta = Math.cos(st[2]);
        double poleOneSinTheta = Math.sin(st[2]);

        double ml_1 = data.poleOneLength() * data.poleOneMass();
        double temp_1 = data.pivotFriction() * st[3] / ml_1;

        double fi_1 = (ml_1 * st[3] * st[3] * poleOneSinTheta)
                + (0.75 * data.poleOneMass() * poleOneCosTheta * (temp_1 + (data.gravity() * poleOneSinTheta)));


        double mi_1 = data.poleOneMass() * (1 - (0.75 * poleOneCosTheta * poleOneCosTheta));

        derivations[1] = (force + fi_1) / (mi_1 + data.cartMass());
        derivations[3] = -0.75 * (derivations[1] * poleOneCosTheta + (data.gravity() * poleOneSinTheta) + temp_1) / data.poleOneLength();
    }

    private void rk4OnePole(double f, double[] y, double[] dydx, double[] yout) {
        int i;

        double hh, h6;
        double[] dym = new double[4];
        double[] dyt = new double[4];
        double[] yt = new double[4];

        hh = data.timeDelta() * 0.5;
        h6 = data.timeDelta() / 6.0;
        for (i = 0; i <= 3; i++)
            yt[i] = y[i] + hh * dydx[i];
        stepOnePole(f, yt, dyt);
        dyt[0] = yt[1];
        dyt[2] = yt[3];
        for (i = 0; i <= 3; i++)
            yt[i] = y[i] + hh * dyt[i];
        stepOnePole(f, yt, dym);
        dym[0] = yt[1];
        dym[2] = yt[3];
        for (i = 0; i <= 3; i++) {
            yt[i] = y[i] + data.timeDelta() * dym[i];
            dym[i] += dyt[i];
        }
        stepOnePole(f, yt, dyt);
        dyt[0] = yt[1];
        dyt[2] = yt[3];
        for (i = 0; i <= 3; i++)
            yout[i] = y[i] + h6 * (dydx[i] + dyt[i] + 2.0 * dym[i]);
    }

    private void checkDied() {
        this.dead |= Math.abs(cartPosition) > (data.trackLength() / 2);
        this.dead |= Math.abs(this.poleOneAngle) > data.poleAngleThreshold();

        if (data.twoPoles()) {
            this.dead |= Math.abs(this.poleTwoAngle) > data.poleAngleThreshold();
        }
    }

    public void configure(GeneralCartPoleData data) {
        this.data = data;

        if (data.randomStartAngle()) {
            NormalDistribution distribution = new NormalDistribution();
            this.poleOneAngle = distribution.sample() * data.poleOneStartAngle();
            this.poleTwoAngle = distribution.sample() * data.poleTwoStartAngle();
        } else {
            this.poleOneAngle = data.poleOneStartAngle();
            this.poleTwoAngle = data.poleTwoStartAngle();
        }
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

    public double getStep() {
        return this.step;
    }

    public void incrementTimeStep() {
        this.step += 1;
    }

    public void setEnvironment(CartPoleEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((CartShape) this.getShape()).getShapesForElement(this);
    }

    public double getPoleOneAngle() {
        return poleOneAngle;
    }

    public double getPoleTwoAngle() {
        return poleTwoAngle;
    }

    public GeneralCartPoleData getPoleData() {
        return this.data;
    }

    public double getFitness() {
        return this.fitness;
    }
}
