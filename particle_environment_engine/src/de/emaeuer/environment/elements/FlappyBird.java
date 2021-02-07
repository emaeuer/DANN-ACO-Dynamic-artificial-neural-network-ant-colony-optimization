package de.emaeuer.environment.elements;

import de.emaeuer.environment.impl.FlappyBirdEnvironment;
import de.emaeuer.environment.math.Vector2D;
import de.emaeuer.optimization.Solution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class FlappyBird extends Particle {

    private FlappyBirdEnvironment environment;

    private boolean dead = false;

    private double score = 0;

    private Solution solution;

    private void jump() {
        // disable jump if bird is already going upwards
        if (getVelocity().getY() >= 0 && !isDead()) {
            getVelocity().multiply(0);
            applyForce(new Vector2D(0, -100));
        }
    }

    @Override
    public void step() {
        if (getScore() > 5000) {
            setDead(true);
        }

        if (isDead() || this.environment == null) {
            return;
        }

        super.step();

        // create input vector and normalize input parameters
        double currentHeight = getPosition().getY() / this.environment.getHeight();
        double yVelocity = getVelocity().getY() / getMaxVelocity();
        double nextGapHeight = this.environment.getHeightOfNextGap() / this.environment.getHeight();
        double distanceToNextPipe = this.environment.getDistanceToNextPipeEnd() / this.environment.getWidth();
        RealVector input = new ArrayRealVector(new double[]{currentHeight, yVelocity, nextGapHeight, distanceToNextPipe});

        // process input and let the neural network decide when to jump
        double output = this.solution.process(input).getEntry(0);
        if (output > 0.9) {
            jump();
        }

        if (this.environment.collidesWithNextPipe(this)) {
            setDead(true);
        }
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
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
        this.score += 0.1;
        this.solution.setFitness(this.score);
    }

    public void setEnvironment(FlappyBirdEnvironment environment) {
        this.environment = environment;
    }
}
