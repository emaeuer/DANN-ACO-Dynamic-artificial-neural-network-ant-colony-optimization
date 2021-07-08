package de.emaeuer.environment.bird.elements;

import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.elements.Particle;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.math.Vector2D;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class FlappyBird extends Particle {

    private FlappyBirdEnvironment environment;

    private boolean dead = false;

    private double score = 0;

    private AgentController controller;

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

        // create input vector and normalize input parameters
        double currentHeight = getPosition().getY() / this.environment.getHeight();
        double yVelocity = getVelocity().getY() / getMaxVelocity();
        double nextGapHeight = this.environment.getHeightOfNextGap() / this.environment.getHeight();
        double distanceToNextPipe = this.environment.getDistanceToNextPipeEnd() / this.environment.getWidth();
        double[] input = new double[]{currentHeight, yVelocity, nextGapHeight, distanceToNextPipe};

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
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void incrementScore() {
        this.score += 1;
        this.controller.setScore(this.score);
    }

    public void setEnvironment(FlappyBirdEnvironment environment) {
        this.environment = environment;
    }
}
