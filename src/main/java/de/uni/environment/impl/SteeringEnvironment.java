package de.uni.environment.impl;

import de.uni.environment.elements.Particle;
import de.uni.environment.AbstractEnvironment;
import de.uni.environment.elements.builder.ParticleBuilder;
import de.uni.environment.util.ForceHelper;
import de.uni.environment.elements.builder.ElementBuilder;
import de.uni.math.Vector2D;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class SteeringEnvironment extends AbstractEnvironment {

    private ObjectProperty<Vector2D> target;

    public SteeringEnvironment(int particleNumber) {
        super(particleNumber);
    }

    public SteeringEnvironment(int particleNumber, BiConsumer<Particle, AbstractEnvironment> borderStrategy) {
        super(particleNumber, borderStrategy);
    }

    @Override
    protected void initialize() {
        this.target = new SimpleObjectProperty<>(new Vector2D());
    }

    @Override
    protected void initializeParticles() {
        Random generator = new Random();
        IntStream.range(0, getParticleNumber())
                .mapToObj(i -> new ParticleBuilder())
                .map(builder -> builder.radius(generator.nextInt(4) + 4))
                .map(builder -> builder.setStartPosition(
                        generator.nextInt(Double.valueOf(getWidth()).intValue()),
                        generator.nextInt(Double.valueOf(getHeight()).intValue())))
                .map(builder -> builder.maxVelocity(5))
                .map(builder -> builder.addPermanentForce(ForceHelper.createSteeringForce(getTarget(), generator.nextDouble() * 0.6 + 0.4,100)))
                .map(builder -> builder.initialImpulse(ForceHelper.createBasicForce(new Vector2D(100,100))))
                .map(builder -> builder.color(0, 0,0, 0.5 ))
                .map(ElementBuilder::build)
                .forEach(getParticles()::add);
    }

    public Vector2D getTarget() {
        return target.get();
    }

    public ObjectProperty<Vector2D> targetProperty() {
        return target;
    }
}
