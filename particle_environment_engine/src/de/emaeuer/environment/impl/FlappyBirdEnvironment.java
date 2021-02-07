package de.emaeuer.environment.impl;

import de.emaeuer.aco.AcoHandler;
import de.emaeuer.aco.Ant;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.elements.FlappyBird;
import de.emaeuer.environment.elements.Particle;
import de.emaeuer.environment.elements.Pipe;
import de.emaeuer.environment.elements.builder.ElementBuilder;
import de.emaeuer.environment.elements.builder.FlappyBirdBuilder;
import de.emaeuer.environment.elements.builder.PipeBuilder;
import de.emaeuer.environment.math.Vector2D;
import de.emaeuer.environment.util.ForceHelper;
import de.emaeuer.optimization.OptimizationMethod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

import java.util.*;
import java.util.function.BiConsumer;

public class FlappyBirdEnvironment extends AbstractEnvironment {

    private static final int BIRD_X = 80;
    private static final int BIRD_SIZE = 20;

    private static final BiConsumer<Particle, AbstractEnvironment> DIE_ON_BORDER = (particle, env) -> {
        if (particle instanceof FlappyBird bird && !bird.isDead()) {
            boolean isDead = bird.getPosition().getY() + bird.getRadius() > env.getHeight();
            isDead |= particle.getPosition().getY() - particle.getRadius() <= 0;

            if (particle.getPosition().getY() - particle.getRadius() <= 0) {
                bird.setScore(bird.getScore() * 0.5);
            }

            bird.setDead(isDead);
        }
    };

    private final BooleanProperty allBirdsDead = new SimpleBooleanProperty(this, "allBirdsDead", false);

    private final int gapSize;
    private final int pipeWidth;
    private final int pipeDistance;

    private final List<Pipe> pipes = new ArrayList<>();

    private final List<FlappyBird> deadBirds = new ArrayList<>();

    private OptimizationMethod optimization;

    public FlappyBirdEnvironment(int gapSize, int pipeWidth, int pipeDistance) {
        super(100, DIE_ON_BORDER);
        this.gapSize = gapSize;
        this.pipeWidth = pipeWidth;
        this.pipeDistance = pipeDistance;
    }

    @Override
    protected void initialize() {
        this.optimization = new AcoHandler(4, 1, 5, 20);
    }

    Map<Integer, Integer[]> colors;

    @Override
    protected void initializeParticles() {

        if (this.colors == null) {
            this.colors = new HashMap<>();
        }

        this.optimization.generateSolutions()
                .stream()
                .map(sol -> {
                    FlappyBirdBuilder builder = new FlappyBirdBuilder();
                    builder.solution(sol);
                    return new AbstractMap.SimpleEntry<>(builder, sol);
                })
                .map(b -> {
                    if (b.getValue() instanceof Ant ant) {
                        Random rand = new Random();
                        Integer[] color = colors.computeIfAbsent(ant.getColonyNumber(), i -> new Integer[]{rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)});
                        return b.getKey().color(color[0],color[1],color[2],0.5);
                    } else {
                        return b.getKey().color(0,0,0,0.5);
                    }
                })
                .map(builder -> builder.radius(BIRD_SIZE))
                .map(builder -> builder.environment(this))
                .map(builder -> builder.setStartPosition(BIRD_X, getHeight() / 2))
                .map(builder -> builder.maxVelocity(15))
                .map(builder -> builder.addPermanentForce(ForceHelper.createBasicForce(new Vector2D(0, 2))))
                .map(ElementBuilder::build)
                .forEach(getParticles()::add);
    }

    @Override
    public void restart() {
        this.optimization.update();
        updateFitness();

        this.deadBirds.clear();
        this.pipes.clear();
        initializeParticles();
        setAllBirdsDead(false);
    }

    @Override
    protected void updateFitness() {
        for (int i = 0; i < this.optimization.getStatisticsOfIteration().size(); i++) {
            DoubleSummaryStatistics fitness = this.optimization.getStatisticsOfIteration().get(i);

            // add new series if none exists
            if (getFitnessData().size() == i) {
                Series<Integer, Double> series = new Series<>(String.format("ACO-Colony %d", i), FXCollections.observableArrayList());
                getFitnessData().add(series);
            }

            // add new data point
            getFitnessData().get(i).getData().add(new Data<>(this.optimization.getIterationCount(), fitness.getMax()));
        }
    }

    @Override
    public void update() {
        if (areAllBirdsDead()) {
            return;
        }

        super.update();
        updatePipes();

        // increment scores and check if at least one bird lives
        getParticles().stream()
                .filter(FlappyBird.class::isInstance)
                .map(FlappyBird.class::cast)
                .peek(FlappyBird::incrementScore)
                .filter(FlappyBird::isDead)
                .forEach(this.deadBirds::add);


        getParticles().removeAll(deadBirds);
        setAllBirdsDead(getParticles().isEmpty());
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
                .gapPosition(Math.random() * (getHeight() - 60 - this.gapSize) + 30)
                .gapSize(this.gapSize)
                .color(0, 255,0)
                .build();
    }

    public List<Pipe> getPipes() {
        return pipes;
    }

    public int getGapSize() {
        return gapSize;
    }

    public int getPipeWidth() {
        return pipeWidth;
    }

    public int getPipeDistance() {
        return pipeDistance;
    }

    public boolean areAllBirdsDead() {
        return allBirdsDead.get();
    }

    public BooleanProperty allBirdsDeadProperty() {
        return allBirdsDead;
    }

    public void setAllBirdsDead(boolean allBirdsDead) {
        this.allBirdsDead.set(allBirdsDead);
    }
}
