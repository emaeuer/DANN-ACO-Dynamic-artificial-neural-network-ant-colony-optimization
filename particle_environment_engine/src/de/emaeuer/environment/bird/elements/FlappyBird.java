package de.emaeuer.environment.bird.elements;

import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.elements.Particle;
import de.emaeuer.environment.bird.FlappyBirdEnvironment;
import de.emaeuer.environment.math.Vector2D;
import de.emaeuer.optimization.Solution;
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
        RealVector input = new ArrayRealVector();

        // process input and let the neural network decide when to jump
        boolean jump = this.controller.getAction(new double[]{currentHeight, yVelocity, nextGapHeight, distanceToNextPipe}) == 1;
        if (jump) {
            jump();
        }

        // check solution died
        if (this.environment.collidesWithNextPipe(this)) {
            setDead(true);
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
