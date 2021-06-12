package de.emaeuer.environment.bird;

import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.bird.configuration.FlappyBirdConfiguration;
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

public class FlappyBirdEnvironment extends AbstractEnvironment {

    private static final int BIRD_X = 80;
    private static final int BIRD_SIZE = 20;

    private static final BiConsumer<AbstractElement, AbstractEnvironment> DIE_ON_BORDER = (particle, env) -> {
        if (particle instanceof FlappyBird bird && !bird.isDead()) {
            boolean isDead = bird.getPosition().getY() + bird.getRadius() > env.getHeight();
            isDead |= particle.getPosition().getY() - bird.getRadius() <= 0;

            if (particle.getPosition().getY() - bird.getRadius() <= 0) {
                bird.setScore(bird.getScore() * 0.5);
            }

            bird.setDead(isDead);
        }
    };

    private boolean areAllBirdsDead = false;

    private final int gapSize;
    private final int pipeWidth;
    private final int pipeDistance;

    private final List<Pipe> pipes = new ArrayList<>();

    private Random randomGenerator = new Random( 9369319);

    private Map<Integer, Integer[]> colors;
    private FlappyBird bestParticle;

    public FlappyBirdEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(DIE_ON_BORDER, configuration);

        ConfigurationHandler<FlappyBirdConfiguration> implementationConfiguration =
                ConfigurationHelper.extractEmbeddedConfiguration(configuration, FlappyBirdConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);

        this.gapSize = implementationConfiguration.getValue(FlappyBirdConfiguration.GAP_SIZE, Integer.class);
        this.pipeWidth = implementationConfiguration.getValue(FlappyBirdConfiguration.PIPE_WIDTH, Integer.class);
        this.pipeDistance = implementationConfiguration.getValue(FlappyBirdConfiguration.PIPE_DISTANCE, Integer.class);
    }

    @Override
    protected void initializeParticles(List<AgentController> controllers) {
        controllers.stream()
                .map(this::buildFlappyBird)
                .forEach(getAgents()::add);
    }

    private FlappyBird buildFlappyBird(AgentController controller) {
        FlappyBirdBuilder builder = new FlappyBirdBuilder()
                .controller(controller)
                .radius(BIRD_SIZE)
                .environment(this)
                .setStartPosition(BIRD_X, getHeight() / 2)
                .maxVelocity(15)
                .addPermanentForce(ForceHelper.createBasicForce(new Vector2D(0, 2)));

        assignColor(builder);

        return builder.build();
    }

    private void assignColor(FlappyBirdBuilder builder) {
       if (this.colors == null) {
           this.colors = new HashMap<>();
       }

       builder.color(0,0,0,0.5);
    }

    @Override
    public void restart() {
        randomGenerator = new Random(9369319);
        this.areAllBirdsDead = false;
        this.pipes.clear();
    }

    @Override
    public void step() {
        super.step();

        updatePipes();

        // increment scores and check if at least one bird lives (ignore this.bestParticle)
        List<FlappyBird> deadBirds = new ArrayList<>();
        getAgents().stream()
                .filter(p -> p != this.bestParticle)
                .filter(FlappyBird.class::isInstance)
                .map(FlappyBird.class::cast)
                .peek(FlappyBird::incrementScore)
                .peek(this::checkReachedMaximumFitness)
                .filter(FlappyBird::isDead)
                .forEach(deadBirds::add);

        if (this.bestParticle != null && this.bestParticle.isDead()) {
            getAgents().remove(this.bestParticle);
        }

        // terminate iteration if only this.bestParticle remains
        if (getAgents().size() == 1 && getAgents().get(0) == this.bestParticle) {
            getAgents().clear();
        }

        getAgents().removeAll(deadBirds);
        this.areAllBirdsDead = getAgents().isEmpty();
    }

    private void checkReachedMaximumFitness(FlappyBird bird) {
        if (bird.getScore() >= getMaxFitnessScore()) {
            bird.setDead(true);
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
                .initialImpulse(p -> p.applyForce(new Vector2D(-1, 0)))
                .gapPosition(this.randomGenerator.nextDouble() * (getHeight() - 60 - this.gapSize) + 30)
                .gapSize(this.gapSize)
                .color(0, 255,0)
                .build();
    }

    @Override
    public List<AbstractElement> getAdditionalEnvironmentElements() {
        List<AbstractElement> elements = super.getAdditionalEnvironmentElements();
        elements.addAll(getPipes());
        return elements;
    }

    @Override
    public boolean allAgentsFinished() {
        return this.areAllBirdsDead;
    }

    public List<Pipe> getPipes() {
        return pipes;
    }

}
