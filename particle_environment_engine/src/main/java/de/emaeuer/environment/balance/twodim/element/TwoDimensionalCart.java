package de.emaeuer.environment.balance.twodim.element;

import com.google.common.primitives.Doubles;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.balance.twodim.GeneralTwoDimensionalCartPoleData;
import de.emaeuer.environment.balance.twodim.TwoDimensionalCartPoleEnvironment;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import de.emaeuer.environment.balance.twodim.element.shape.TwoDimensionalCartShape;

import java.util.ArrayList;
import java.util.List;

public class TwoDimensionalCart extends AbstractElement {

    private TwoDimensionalCartPoleEnvironment environment;

    private GeneralTwoDimensionalCartPoleData data;

    private boolean dead = false;

    private int step = 0;

    private double cartXPosition = 0;
    private double cartYPosition = 0;
    private double cartXVelocity = 0;
    private double cartYVelocity = 0;
    private double poleXAngle = 0;
    private double poleYAngle = 0;
    private double poleXVelocity = 0;
    private double poleYVelocity = 0;

    private AgentController controller;

    public TwoDimensionalCart() {
        super(new TwoDimensionalCartShape());
    }

    @Override
    public void step() {
        if (isDead()) {
            return;
        }

        // calculate the data of the agent
        double[] input = createNetworkInput();

        // get the action of the agent controller
        double[] activation = this.controller.getAction(input);
        adjustActivation(activation, controller);

        updateXState(activation[0]);
        updateYState(activation[1]);

        getPosition().setX(this.cartXPosition + this.environment.getWidth() / 2);
        getPosition().setY(this.cartYPosition + this.environment.getHeight() / 2);

        // check solution died
        checkDied();
        this.controller.setScore(getStep() + 1);
    }


    private void adjustActivation(double[] activation, AgentController controller) {
        // 10 and -10 are arbitrary bounds in case of unlimited values
        double maxActivation = Math.min(controller.getMaxAction(), 10);
        double minActivation = Math.max(controller.getMinAction(), -10);
        double activationRange = maxActivation - minActivation;
        double middle = (maxActivation + minActivation) / 2;

        for (int i = 0; i < activation.length; i++) {
            if (data.binaryForce()) {
                activation[i] = activation[i] > middle ? 1 : -1;
            } else {
                // adjust to [-1:1]
                activation[i] = (2 * (activation[i] - middle)) / activationRange;
            }
        }
    }

    private double[] createNetworkInput() {
        List<Double> inputs = new ArrayList<>();

        if (data.positionInput()) {
            inputs.add(this.cartXPosition / (data.trackLength() / 2));
            inputs.add(this.cartYPosition / (data.trackLength() / 2));
        }

        inputs.add(poleXAngle / data.poleAngleThreshold());
        inputs.add(poleYAngle / data.poleAngleThreshold());

        if (data.velocityInput()) {
            inputs.add(cartXVelocity / 0.75);
            inputs.add(cartYVelocity / 0.75);
            inputs.add(poleXVelocity);
            inputs.add(poleYVelocity);
        }

        return Doubles.toArray(inputs);
    }

    private void updateXState(double activation) {
        double[] dydx = new double[4];
        for (int i = 0; i < 2; i++) {
            dydx[0] = cartXVelocity;
            dydx[2] = poleXVelocity;

            double[] state = new double[]{cartXPosition, cartXVelocity, poleXAngle, poleXVelocity};
            step(activation, state, dydx);
            rk4(activation, state, dydx, state);

            this.cartXPosition = state[0];
            this.cartXVelocity = state[1];
            this.poleXAngle = state[2];
            this.poleXVelocity = state[3];
        }
    }

    private void updateYState(double activation) {
        double[] dydx = new double[4];
        for (int i = 0; i < 2; i++) {
            dydx[0] = cartYVelocity;
            dydx[2] = poleYVelocity;

            double[] state = new double[]{cartYPosition, cartYVelocity, poleYAngle, poleYVelocity};
            step(activation, state, dydx);
            rk4(activation, state, dydx, state);

            this.cartYPosition = state[0];
            this.cartYVelocity = state[1];
            this.poleYAngle = state[2];
            this.poleYVelocity = state[3];
        }
    }

    private void step(double action, double[] st, double[] derivations) {
        double force = action * data.forceMagnitude();

        double poleCosTheta = Math.cos(st[2]);
        double poleSinTheta = Math.sin(st[2]);

        double ml_1 = data.poleLength() * data.poleMass();
        double temp_1 = data.pivotFriction() * st[3] / ml_1;

        double fi_1 = (ml_1 * st[3] * st[3] * poleSinTheta)
                + (0.75 * data.poleMass() * poleCosTheta * (temp_1 + (data.gravity() * poleSinTheta)));


        double mi_1 = data.poleMass() * (1 - (0.75 * poleCosTheta * poleCosTheta));

        derivations[1] = (force + fi_1) / (mi_1 + data.cartMass());
        derivations[3] = -0.75 * (derivations[1] * poleCosTheta + (data.gravity() * poleSinTheta) + temp_1) / data.poleLength();
    }

    private void rk4(double f, double[] y, double[] dydx, double[] yout) {
        int i;

        double hh, h6;
        double[] dym = new double[4];
        double[] dyt = new double[4];
        double[] yt = new double[4];

        hh = data.timeDelta() * 0.5;
        h6 = data.timeDelta() / 6.0;
        for (i = 0; i <= 3; i++)
            yt[i] = y[i] + hh * dydx[i];
        step(f, yt, dyt);
        dyt[0] = yt[1];
        dyt[2] = yt[3];
        for (i = 0; i <= 3; i++)
            yt[i] = y[i] + hh * dyt[i];
        step(f, yt, dym);
        dym[0] = yt[1];
        dym[2] = yt[3];
        for (i = 0; i <= 3; i++) {
            yt[i] = y[i] + data.timeDelta() * dym[i];
            dym[i] += dyt[i];
        }
        step(f, yt, dyt);
        dyt[0] = yt[1];
        dyt[2] = yt[3];
        for (i = 0; i <= 3; i++)
            yout[i] = y[i] + h6 * (dydx[i] + dyt[i] + 2.0 * dym[i]);
    }

    private boolean checkDied() {
        this.dead |= Math.abs(cartXPosition) > (data.trackLength() / 2);
        this.dead |= Math.abs(cartYPosition) > (data.trackLength() / 2);
        this.dead |= Math.abs(this.poleXAngle) > data.poleAngleThreshold();
        this.dead |= Math.abs(this.poleYAngle) > data.poleAngleThreshold();

        return this.dead;
    }

    public void configure(GeneralTwoDimensionalCartPoleData data) {
        this.data = data;

        this.poleXAngle = data.poleXStartAngle();
        this.poleYAngle = data.poleYStartAngle();
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

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((TwoDimensionalCartShape) this.getShape()).getShapesForElement(this);
    }

    public AgentController getController() {
        return this.controller;
    }

    public double getPoleXAngle() {
        return poleXAngle;
    }

    public double getPoleYAngle() {
        return poleYAngle;
    }

    public GeneralTwoDimensionalCartPoleData getData() {
        return data;
    }

    public TwoDimensionalCartPoleEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(TwoDimensionalCartPoleEnvironment environment) {
        this.environment = environment;
    }
}
