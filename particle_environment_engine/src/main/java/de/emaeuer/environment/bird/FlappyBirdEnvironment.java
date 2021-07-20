package de.emaeuer.environment.bird;

import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.GeneralizationHandler;
import de.emaeuer.environment.bird.configuration.FlappyBirdConfiguration;
import de.emaeuer.environment.bird.configuration.FlappyBirdGeneralizationConfiguration;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.bird.elements.FlappyBird;
import de.emaeuer.environment.bird.elements.Pipe;
import de.emaeuer.environment.bird.elements.builder.FlappyBirdBuilder;
import de.emaeuer.environment.bird.elements.builder.PipeBuilder;
import de.emaeuer.environment.math.Vector2D;
import de.emaeuer.environment.util.ForceHelper;

import java.util.*;
import java.util.function.BiConsumer;

public class FlappyBirdEnvironment extends AbstractEnvironment<FlappyBirdGeneralizationConfiguration> {

    private static final int BIRD_X = 80;
    private static final int BIRD_SIZE = 20;

    private static final BiConsumer<AbstractElement, AbstractEnvironment<FlappyBirdGeneralizationConfiguration>> DIE_ON_BORDER = (particle, env) -> {
        if (particle instanceof FlappyBird bird && !bird.isDead()) {
            boolean isDead = bird.getPosition().getY() + bird.getRadius() > env.getHeight();
            isDead |= particle.getPosition().getY() - bird.getRadius() <= 0;
            bird.setDead(isDead);
        }
    };

    private boolean finished = false;

    private int gapSize;
    private int pipeWidth;
    private int pipeDistance;
    private int pipeVelocity;
    private int jumpForce;
    private int gravity;

    private final List<Pipe> pipes = new ArrayList<>();

    private final ConfigurationHandler<FlappyBirdConfiguration> configuration;
    private final ConfigurationHandler<FlappyBirdGeneralizationConfiguration> generalizationConfig;

    public FlappyBirdEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(DIE_ON_BORDER, configuration);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, FlappyBirdConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);
        if (configuration.getValue(EnvironmentConfiguration.TEST_GENERALIZATION, Boolean.class)) {
            this.generalizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(configuration, FlappyBirdGeneralizationConfiguration.class, EnvironmentConfiguration.GENERALIZATION_IMPLEMENTATION);
        } else {
            this.generalizationConfig = null;
        }

        setEnvironmentParameters();
    }

    @Override
    protected void initializeParticles(List<AgentController> controllers) {
        initializeParticles(controllers, getHeight() * 0.5);
    }

    protected void initializeParticles(List<AgentController> controllers, double initialHeight) {
        controllers.stream()
                .map(c -> buildFlappyBird(c, initialHeight))
                .forEach(getAgentsToDraw()::add);
    }

    private FlappyBird buildFlappyBird(AgentController controller, double initialHeight) {
        FlappyBirdBuilder builder = new FlappyBirdBuilder()
                .applyConfiguration(this.configuration)
                .controller(controller)
                .radius(BIRD_SIZE)
                .environment(this)
                .setStartPosition(BIRD_X, initialHeight)
                .maxVelocity(30)
                .jumpForce(this.jumpForce)
                .addPermanentForce(ForceHelper.createBasicForce(new Vector2D(0, this.gravity)));

        assignColor(builder);

        return builder.build();
    }

    private void assignColor(FlappyBirdBuilder builder) {
       builder.color(0,0,0,0.5);
    }

    @Override
    public void restart() {
        super.restart();
        this.finished = false;
        this.pipes.clear();

        setEnvironmentParameters();
    }

    private void setEnvironmentParameters() {
        this.gapSize = this.configuration.getValue(FlappyBirdConfiguration.GAP_SIZE, Integer.class);
        this.pipeWidth = this.configuration.getValue(FlappyBirdConfiguration.PIPE_WIDTH, Integer.class);
        this.pipeDistance = this.configuration.getValue(FlappyBirdConfiguration.PIPE_DISTANCE, Integer.class);
        this.pipeVelocity = -1 * this.configuration.getValue(FlappyBirdConfiguration.PIPE_VELOCITY, Integer.class);
        this.gravity = this.configuration.getValue(FlappyBirdConfiguration.GRAVITY, Integer.class);
        this.jumpForce = -1 * this.configuration.getValue(FlappyBirdConfiguration.JUMP_FORCE, Integer.class);

        if (isTestingGeneralization()) {
            getRNG().reset((int) getGeneralizationHandler().getNextValue(FlappyBirdGeneralizationConfiguration.NUMBER_OF_SEEDS));
            this.gapSize *= getGeneralizationHandler().getNextValue(FlappyBirdGeneralizationConfiguration.GAP_SIZES);
            this.pipeWidth *= getGeneralizationHandler().getNextValue(FlappyBirdGeneralizationConfiguration.PIPE_WIDTHS);
            this.pipeDistance *= getGeneralizationHandler().getNextValue(FlappyBirdGeneralizationConfiguration.PIPE_DISTANCES);
        }
    }

    @Override
    public void step() {
        super.step();

        updatePipes();

        // increment scores and check if at least one bird lives
        List<FlappyBird> deadBirds = new ArrayList<>();
        getAgentsToDraw().stream()
                .filter(FlappyBird.class::isInstance)
                .map(FlappyBird.class::cast)
                .peek(FlappyBird::incrementScore)
                .peek(this::checkBird)
                .filter(FlappyBird::isDead)
                .peek(b -> b.setScore(b.getScore() / getMaxStepNumber()))
                .peek(b -> getOriginControllers().remove(b.getController()))
                .forEach(deadBirds::add);

        getAgentsToDraw().removeAll(deadBirds);

        this.finished = getAgentsToDraw().isEmpty();
    }

    private void checkBird(FlappyBird bird) {
        if (bird.getScore() >= getMaxStepNumber()) {
            bird.setDead(true);
            setControllerFinishedWithoutDying(true);

            handleGeneralizationChecks(bird);
        }
    }

    private void handleGeneralizationChecks(FlappyBird bird) {
        if (isTestingGeneralization()) {
            AgentController origin = getOriginControllers().get(bird.getController());
            origin.setGeneralizationCapability(origin.getGeneralizationCapability() + (1.0 / getGeneralizationHandler().getNumberOfGeneralizationIterations()));
            setCurrentGeneralizationCapability(Math.max(getCurrentGeneralizationProgress(), origin.getGeneralizationCapability()));
        }
    }

    private void updatePipes() {
        if (pipes.isEmpty()) {
            this.pipes.add(addStandardPipe());
        }

        Pipe firstPipe = pipes.get(0);
        if (firstPipe.getPosition().getX() < firstPipe.getWidth() * -1) {
            pipes.remove(firstPipe);
        }

        Pipe lastPipe = pipes.get(pipes.size() - 1);
        if (lastPipe.getPosition().getX() < getWidth() - pipeDistance - firstPipe.getWidth()) {
            this.pipes.add(addStandardPipe());
        }

        this.pipes.forEach(Pipe::step);
    }

    public double getDistanceToNextPipeEnd() {
        return getNextPipe().getPosition().getX() + this.pipeWidth + BIRD_SIZE - BIRD_X;
    }

    public double getHeightOfNextGap() {
        return getNextPipe().getGapPosition() + getNextPipe().getGapSize() / 2;
    }

    public boolean collidesWithNextPipe(FlappyBird flappyBird) {
        Pipe nextPipe = getNextPipe();

        double particleY = flappyBird.getPosition().getY();
        double pipeX = nextPipe.getPosition().getX();
        double pipeWidth = nextPipe.getWidth();
        double gapY = nextPipe.getGapPosition();
        double gapSize = nextPipe.getGapSize();

        // particle reached the pipe
        boolean collision = BIRD_X + BIRD_SIZE > pipeX && BIRD_X - BIRD_SIZE < pipeX + pipeWidth;
        // particle is not in the gap
        collision &= particleY - BIRD_SIZE < gapY || particleY + BIRD_SIZE > gapY + gapSize;

        return collision;
    }

    private Pipe getNextPipe() {
        for (Pipe pipe: this.pipes) {
            if (BIRD_X - BIRD_SIZE < pipe.getPosition().getX() + pipeWidth) {
                return pipe;
            }
        }
        // if all pipes are behind bird add new pipe --> should not happen
        Pipe pipe = addStandardPipe();
        this.pipes.add(addStandardPipe());
        return pipe;
    }

    private Pipe addStandardPipe() {
        return new PipeBuilder()
                .setStartPosition(getWidth(), 0)
                .size(new Vector2D(this.pipeWidth, getHeight()))
                .initialImpulse(p -> p.applyForce(new Vector2D(this.pipeVelocity, 0)))
                .gapPosition(getRNG().nextDouble() * (getHeight() - 60 - this.gapSize) + 30)
                .gapSize(this.gapSize)
                .color(0, 255,0)
                .build();
    }

    @Override
    protected GeneralizationHandler<FlappyBirdGeneralizationConfiguration> getNewGeneralizationHandler() {
        return new FlappyBirdGeneralizationHandler(this.generalizationConfig, getRNG());
    }

    @Override
    public void nextGeneralizationIteration() {
        super.nextGeneralizationIteration();
        this.pipes.clear();

        if (finishedGeneralization()) {
            return;
        }

        setEnvironmentParameters();
        getGeneralizationHandler().next();

        List<Double> heights = ConfigurationHelper.getNumericListValue(this.generalizationConfig, FlappyBirdGeneralizationConfiguration.BIRD_START_HEIGHTS);

        for (double height : heights) {
            List<AgentController> copyControllers = new ArrayList<>();
            for (AgentController agent : getAgents()) {
                AgentController copy = agent.copy();
                copyControllers.add(copy);
                getOriginControllers().put(copy, agent);
            }

            initializeParticles(copyControllers, getHeight() * height);
        }
    }

    @Override
    public List<AbstractElement> getAdditionalEnvironmentElements() {
        List<AbstractElement> elements = super.getAdditionalEnvironmentElements();
        elements.addAll(getPipes());
        return elements;
    }

    @Override
    public boolean environmentFinished() {
        return this.finished;
    }

    public List<Pipe> getPipes() {
        return pipes;
    }
}
