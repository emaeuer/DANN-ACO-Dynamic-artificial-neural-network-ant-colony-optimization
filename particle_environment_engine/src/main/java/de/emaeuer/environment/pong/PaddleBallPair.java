package de.emaeuer.environment.pong;

import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.math.Vector2D;
import de.emaeuer.environment.pong.configuration.PongConfiguration;
import de.emaeuer.environment.pong.elements.Ball;
import de.emaeuer.environment.pong.elements.Paddle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PaddleBallPair {

    private final Paddle paddle;
    private final Ball ball;
    private final AgentController controller;
    private final PongEnvironment environment;

    private final double maxReflectionAngle;

    public PaddleBallPair(Paddle paddle, Ball ball, AgentController controller, PongEnvironment environment) {
        this.paddle = paddle;
        this.ball = ball;
        this.controller = controller;
        this.environment = environment;

        this.maxReflectionAngle = Math.toRadians(this.environment.getConfiguration().getValue(PongConfiguration.BALL_MAX_REFLECTION_ANGLE, Double.class));
    }


    public boolean isDead() {
        return this.ball.isOutOfGame();
    }

    public void setDead(boolean dead) {
        this.ball.setOutOfGame(dead);
    }

    public void step() {
        this.paddle.step();
        ballStep();

        if (checkForMiss()) {
            setDead(true);
            this.controller.setScore(calculateScoreAfterMiss());
        } else {
            double[] input = createInput();

            double activation = this.controller.getAction(input)[0];
            activation = adjustActivation(activation, this.controller);

            applyAction(activation);
        }
    }

    private void ballStep() {
        // if the ball collides with something the step was already made
        if (!adjustBorderCollision()) {
            this.ball.step();
        }
    }

    private boolean adjustBorderCollision() {
        double rightX = this.ball.getPosition().getX() + this.ball.getSize().getX() / 2;
        double upperY = this.ball.getPosition().getY() - this.ball.getSize().getY() / 2;
        double lowerY = this.ball.getPosition().getY() + this.ball.getSize().getY() / 2;
        double xVelocity = this.ball.getVelocity().getX();
        double yVelocity = this.ball.getVelocity().getY();

        boolean hitsTop = upperY + yVelocity < 0 && yVelocity < 0;
        boolean hitsBottom = lowerY + yVelocity > this.environment.getHeight() && yVelocity > 0;
        boolean hitsRight = rightX + xVelocity > this.environment.getWidth() && xVelocity > 0;
        boolean hitsPaddle = checkPaddleCollision();

        if (hitsTop) {
            handleTopHit();
        } else if (hitsBottom) {
            handleBottomHit();
        } else if (hitsRight) {
            handleRightHit();
        } else if (hitsPaddle) {
            handlePaddleHit();
            this.controller.setScore(this.controller.getScore() + 1);
        }

        return hitsTop || hitsBottom || hitsPaddle || hitsRight;
    }

    private void handleTopRightHit() {
        double y = this.ball.getPosition().getY() - this.ball.getSize().getY() / 2;
        double x = this.environment.getWidth() - this.ball.getPosition().getX() + this.ball.getSize().getX() / 2;
        double yVelocity = this.ball.getVelocity().getY();
        double xVelocity = this.ball.getVelocity().getX();

        this.ball.getPosition().setX(this.environment.getWidth() - (1 - x / xVelocity) * xVelocity);
        this.ball.getPosition().setY((1 - y / yVelocity) * yVelocity);
        this.ball.getVelocity().multiply(new Vector2D(-1, -1));
    }

    private void handleBottomRightHit() {
        double y = this.environment.getHeight() - this.ball.getPosition().getY() + this.ball.getSize().getY() / 2;
        double x = this.environment.getWidth() - this.ball.getPosition().getX() + this.ball.getSize().getX() / 2;
        double yVelocity = this.ball.getVelocity().getY();
        double xVelocity = this.ball.getVelocity().getX();

        this.ball.getPosition().setX(this.environment.getWidth() - (1 - x / xVelocity) * xVelocity);
        this.ball.getPosition().setY(this.environment.getHeight() - (1 - y / yVelocity) * yVelocity);
        this.ball.getVelocity().multiply(new Vector2D(-1, -1));
    }

    private void handleTopHit() {
        double y = this.ball.getPosition().getY() - this.ball.getSize().getY() / 2;
        double velocity = this.ball.getVelocity().getY();

        this.ball.getPosition().setX(this.ball.getPosition().getX() + this.ball.getVelocity().getX());
        this.ball.getPosition().setY(-1 * (1 + y / velocity) * velocity + this.ball.getSize().getY() / 2);
        this.ball.getVelocity().multiply(new Vector2D(1, -1));
    }

    private void handleBottomHit() {
        double y = this.environment.getHeight() - (this.ball.getPosition().getY() + this.ball.getSize().getY() / 2);
        double velocity = this.ball.getVelocity().getY();

        this.ball.getPosition().setX(this.ball.getPosition().getX() + this.ball.getVelocity().getX());
        this.ball.getPosition().setY(this.environment.getHeight() + (-1 * (1 - y / velocity) * velocity - this.ball.getSize().getY() / 2));
        this.ball.getVelocity().multiply(new Vector2D(1, -1));
    }

    private void handleRightHit() {
        double x = this.environment.getWidth() - (this.ball.getPosition().getX() + this.ball.getSize().getX() / 2);
        double velocity = this.ball.getVelocity().getX();

        this.ball.getPosition().setX(this.environment.getWidth() + (-1 * (1 - x / velocity) * velocity - this.ball.getSize().getX() / 2));
        this.ball.getPosition().setY(this.ball.getPosition().getY() + this.ball.getVelocity().getY());
        this.ball.getVelocity().multiply(new Vector2D(-1, 1));
    }

    private void handlePaddleHit() {
        double distance = calculateBallPaddleDistance() - this.ball.getSize().getX() / 2;
        double maxVelocity = this.ball.getMaxVelocity();

        // Step one: Go to hit position
        Vector2D velocity = Vector2D.limit(this.ball.getVelocity(), (distance / maxVelocity) * maxVelocity);
        this.ball.getPosition().add(velocity);

        // Step two: Calculate new velocity depending on the offset from the middle
        double offset = this.paddle.getPosition().getY() - this.ball.getPosition().getY();
        offset /= this.paddle.getSize().getY() / 2;

        double reflectionAngle = this.maxReflectionAngle * offset;

        this.ball.getVelocity().setX(maxVelocity * Math.cos(reflectionAngle));
        this.ball.getVelocity().setY(-1 * maxVelocity * Math.sin(reflectionAngle));

        // Step three: Go remaining distance with new velocity
        velocity = Vector2D.limit(this.ball.getVelocity(), (1 - distance / maxVelocity) * maxVelocity);
        this.ball.getPosition().add(velocity);
    }

    private boolean checkPaddleCollision() {
        double ballRadius = this.ball.getSize().getX() / 2;
        double ballMaxVelocity = this.ball.getMaxVelocity();
        double ballXVelocity = this.ball.getVelocity().getX();

        return ballRadius + ballMaxVelocity >= calculateBallPaddleDistance() && ballXVelocity < 0;
    }

    private double calculateBallPaddleDistance() {
        double rightPaddleX = this.paddle.getPosition().getX() + this.paddle.getSize().getX() / 2;
        double upperPaddleY = this.paddle.getPosition().getY() - this.paddle.getSize().getY() / 2;
        double lowerPaddleY = this.paddle.getPosition().getY() + this.paddle.getSize().getY() / 2;

        Vector2D upperPaddle = new Vector2D(rightPaddleX, upperPaddleY);
        Vector2D lowerPaddle = new Vector2D(rightPaddleX, lowerPaddleY);
        Vector2D ballPosition = new Vector2D(this.ball.getPosition());

        double paddleLengthSquared = Math.pow(Vector2D.distance(upperPaddle, lowerPaddle), 2);
        double lineIntersection = Math.max(0, Math.min(1, Vector2D.subtract(ballPosition, upperPaddle).dotProduct(Vector2D.subtract(lowerPaddle, upperPaddle)) / paddleLengthSquared));

        Vector2D nearestPoint = Vector2D.subtract(lowerPaddle, upperPaddle)
                .multiply(lineIntersection)
                .add(upperPaddle);

        return Vector2D.distance(nearestPoint, ballPosition);
    }

    private boolean checkForHit() {
        double ballRadius = this.ball.getSize().getX() / 2;
        double leftBallX = this.ball.getPosition().getX() - ballRadius;
        double rightPaddleX = this.paddle.getPosition().getX() + this.paddle.getSize().getX() / 2;

        if (leftBallX > rightPaddleX) {
            return false;
        }

        double upperPaddleY = this.paddle.getPosition().getY() - this.paddle.getSize().getY() / 2;
        double lowerPaddleY = this.paddle.getPosition().getY() + this.paddle.getSize().getY() / 2;
        double upperBallY = this.ball.getPosition().getY() - ballRadius;
        double lowerBallY = this.ball.getPosition().getY() + ballRadius;

        return upperPaddleY < lowerBallY && lowerPaddleY > upperBallY;
    }

    private boolean checkForMiss() {
        double ballRadius = this.ball.getSize().getX() / 2;
        double rightBallX = this.ball.getPosition().getX() - ballRadius;

        if (rightBallX > 0) {
            return false;
        }

        double upperBallY = this.ball.getPosition().getY() - ballRadius;
        double lowerBallY = this.ball.getPosition().getY() + ballRadius;
        double upperPaddleY = this.paddle.getPosition().getY() - this.paddle.getSize().getY() / 2;
        double lowerPaddleY = this.paddle.getPosition().getY() + this.paddle.getSize().getY() / 2;

        return !(upperPaddleY < lowerBallY && lowerPaddleY > upperBallY);
    }

    private double calculateScoreAfterMiss() {
        double distanceScore = 1 - Math.abs(this.paddle.getPosition().getY() - this.ball.getPosition().getY());

        return this.controller.getScore() + distanceScore / environment.getHeight();
    }

    private double[] createInput() {
        List<Double> input = new ArrayList<>();

        // height of paddle
        input.add(this.paddle.getPosition().getY() / this.environment.getHeight());

        // height of ball
        input.add(this.ball.getPosition().getY() / this.environment.getHeight());

        // x position of the ball
        input.add(this.ball.getPosition().getX() / this.environment.getWidth());

        return input.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
    }

    private double adjustActivation(double activation, AgentController controller) {
        // 10 and -10 are arbitrary bounds in case of unlimited values
        double maxActivation = Math.min(controller.getMaxAction(), 10);
        double minActivation = Math.max(controller.getMinAction(), -10);

        double upperThreshold = minActivation + (2.0 / 3) * (maxActivation - minActivation);
        double lowerThreshold = minActivation + (1.0 / 3) * (maxActivation - minActivation);

        // go up, down or do nothing
        if (activation < lowerThreshold) {
            return -1;
        } else if (activation > upperThreshold) {
            return 1;
        } else {
            return 0;
        }
    }

    private void applyAction(double activation) {
        double paddleVelocity = this.paddle.getMaxVelocity();
        this.paddle.getVelocity().setY(activation * paddleVelocity);
    }

    public long getStepNumber() {
        return this.paddle.getStepNumber();
    }

    public Paddle paddle() {
        return paddle;
    }

    public Ball ball() {
        return ball;
    }

    public AgentController controller() {
        return controller;
    }

    public PongEnvironment environment() {
        return environment;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PaddleBallPair) obj;
        return Objects.equals(this.paddle, that.paddle) &&
                Objects.equals(this.ball, that.ball) &&
                Objects.equals(this.controller, that.controller) &&
                Objects.equals(this.environment, that.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paddle, ball, controller, environment);
    }

    @Override
    public String toString() {
        return "PaddleBallPair[" +
                "paddle=" + paddle + ", " +
                "ball=" + ball + ", " +
                "controller=" + controller + ", " +
                "environment=" + environment + ']';
    }

}
