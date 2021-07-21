package de.emaeuer.environment.bird.elements;

import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.Particle;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.elements.shape.*;
import de.emaeuer.environment.math.Vector2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlappyBird extends AbstractElement {

    private FlappyBirdEnvironment environment;

    private boolean dead = false;

    private AgentController controller;

    private int inputPattern = 0b1111;

    public FlappyBird() {
        super(new FlappyBirdShape());
    }

    private void jump() {
        // disable jump if bird is already going upwards
        if (getVelocity().getY() >= 0 && !isDead()) {
            getVelocity().multiply(0);
            applyForce(new Vector2D(0, -100));
        }
    }

    @Override
    public void step() {
        if (isDead() || this.environment == null) {
            return;
        }

        super.step();

        double[] input = createInput();

        // get the action of the agent controller
        double activation = this.controller.getAction(input)[0];
        activation = adjustActivation(activation, controller);

        if (activation == 1) {
            jump();
        }

        // check solution died
        if (this.environment.collidesWithNextPipe(this)) {
            setDead(true);
        }
    }

    private double[] createInput() {
        List<Double> inputs = new ArrayList<>();

        if ((this.inputPattern & 1) == 1) {
            // input current height
            inputs.add(getPosition().getY() / this.environment.getHeight());
        }

        if (((this.inputPattern >> 1) & 1) == 1) {
            // input y velocity
            inputs.add(getVelocity().getY() / getMaxVelocity());
        }

        if (((this.inputPattern >> 2) & 1) == 1) {
            // input gap height
            inputs.add(this.environment.getHeightOfNextGap() / this.environment.getHeight());
        }

        if (((this.inputPattern >> 3) & 1) == 1) {
            // input distance to next pipe
            inputs.add(this.environment.getDistanceToNextPipeEnd() / this.environment.getWidth());
        }

        return inputs.stream()
                .mapToDouble(v -> v)
                .toArray();
    }

    private double adjustActivation(double activation, AgentController controller) {
        // 10 and -10 are arbitrary bounds in case of unlimited values
        double maxActivation = Math.min(controller.getMaxAction(), 10);
        double minActivation = Math.max(controller.getMinAction(), -10);
        double middle = (maxActivation + minActivation) / 2;

        // either 0 (do nothing) or 1 (jump)
        return activation > middle ? 1 : 0;
    }

    public AgentController getController() {
        return controller;
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
        return this.controller.getScore();
    }

    public void setScore(double score) {
        this.controller.setScore(score);
    }

    public void incrementScore() {
        this.controller.setScore(getScore() + 1);
    }

    public void setEnvironment(FlappyBirdEnvironment environment) {
        this.environment = environment;
    }

    public void setInputPattern(int inputPattern) {
        this.inputPattern = inputPattern;
    }

    public double getRadius() {
        return Math.max(getSize().getX(), getSize().getY()) / 2;
    }

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((FlappyBirdShape) this.getShape()).getShapesForElement(this);
    }
}
