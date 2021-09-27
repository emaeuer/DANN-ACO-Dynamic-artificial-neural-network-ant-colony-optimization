package de.emaeuer.environment.pong;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.GeneralizationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.math.Vector2D;
import de.emaeuer.environment.pong.configuration.PongConfiguration;
import de.emaeuer.environment.pong.configuration.PongGeneralizationConfiguration;
import de.emaeuer.environment.pong.elements.Ball;
import de.emaeuer.environment.pong.elements.Paddle;
import de.emaeuer.environment.pong.elements.builder.BallBuilder;
import de.emaeuer.environment.pong.elements.builder.PaddleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class PongEnvironment extends AbstractEnvironment<PongGeneralizationConfiguration> {

    private static final int PADDLE_X = 30;
    private static final int PADDLE_WIDTH = 20;

    private static final BiConsumer<AbstractElement, AbstractEnvironment<PongGeneralizationConfiguration>> PONG_BORDER_STRATEGY = (element, environment) -> {
        if (element instanceof Paddle paddle) {
            // Stop at upper or lower bound
            double upperY = paddle.getPosition().getY() - paddle.getSize().getY() / 2;
            double lowerY = paddle.getPosition().getY() + paddle.getSize().getY() / 2;

            if (upperY < 0 || lowerY > environment.getHeight()) {
                paddle.getVelocity().setY(0);
            }
        }
    };

    private final List<PaddleBallPair> elements = new ArrayList<>();

    private final ConfigurationHandler<PongConfiguration> configuration;

    private boolean finished = false;

    public PongEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(PONG_BORDER_STRATEGY, configuration);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, PongConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);
    }

    @Override
    protected GeneralizationHandler<PongGeneralizationConfiguration> getNewGeneralizationHandler() {
        return null;
    }

    @Override
    public void restart() {
        super.restart();
        this.finished = false;
    }

    @Override
    public void step() {
        // next step and check if at least one controller is alive

        List<PaddleBallPair> pairsToRemove = new ArrayList<>();

        this.elements.stream()
                .peek(PaddleBallPair::step)
                .peek(p -> checkBorderCase(p.ball()))
                .peek(p -> checkBorderCase(p.paddle()))
                .peek(this::checkController)
                .filter(PaddleBallPair::isDead)
                .peek(pairsToRemove::add)
                .peek(p -> getOriginControllers().remove(p.controller()))
                .forEach(p -> getAgentsToDraw().remove(p.paddle()));

        this.elements.removeAll(pairsToRemove);

        this.finished = getAgentsToDraw().isEmpty();
    }

    private void checkController(PaddleBallPair controller) {
        if (controller.getStepNumber() >= getMaxStepNumber()) {
            controller.setDead(true);
            setControllerFinishedWithoutDying(true);
        }
    }

    @Override
    protected void initializeParticles(List<AgentController> controllers) {
        controllers.forEach(this::buildPairForController);
    }

    private void buildPairForController(AgentController controller) {
        Ball ball = buildBall();
        Paddle paddle = buildPaddle();

        this.elements.add(new PaddleBallPair(paddle, ball, controller, this));

        getAgentsToDraw().add(paddle);
    }

    private Ball buildBall() {
        double ballRadius = this.configuration.getValue(PongConfiguration.BALL_RADIUS, Double.class);
        double maxVelocity = this.configuration.getValue(PongConfiguration.BALL_VELOCITY, Double.class);

        return new BallBuilder()
                .mass(1)
                .setStartPosition(getWidth() / 2, getHeight() / 2)
                .maxVelocity(maxVelocity)
                .startDirection(this.configuration.getValue(PongConfiguration.BALL_ANGLE, Double.class), maxVelocity)
                .size(new Vector2D(ballRadius * 2, ballRadius * 2))
                .build();
    }

    private Paddle buildPaddle() {
        return new PaddleBuilder()
                .maxVelocity(this.configuration.getValue(PongConfiguration.PADDLE_VELOCITY, Double.class))
                .setStartPosition(PADDLE_X, getHeight() / 2)
                .mass(1)
                .size(new Vector2D(PADDLE_WIDTH, this.configuration.getValue(PongConfiguration.PADDLE_HEIGHT, Double.class)))
                .build();
    }

    @Override
    public List<AbstractElement> getAdditionalEnvironmentElements() {
        return this.elements.stream()
                .map(PaddleBallPair::ball)
                .collect(Collectors.toList());
    }

    @Override
    public boolean environmentFinished() {
        return this.finished;
    }

    public ConfigurationHandler<PongConfiguration> getConfiguration() {
        return this.configuration;
    }
}
