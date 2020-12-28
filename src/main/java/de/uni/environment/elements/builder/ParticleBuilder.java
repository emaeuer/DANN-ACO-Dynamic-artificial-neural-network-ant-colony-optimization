package de.uni.environment.elements.builder;

import de.uni.environment.elements.Particle;

public class ParticleBuilder extends ElementBuilder<Particle, ParticleBuilder> {

    public ParticleBuilder() {
        super();
        getElement().setRadius(20);
        getElement().setMass(20);
    }

    @Override
    protected Particle getElementImplementation() {
        return new Particle();
    }

    @Override
    protected ParticleBuilder getThis() {
        return this;
    }

    public ParticleBuilder radius(double radius) {
        getElement().setRadius(radius);
        return getThis();
    }

}
