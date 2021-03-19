package de.emaeuer.environment.elements;

import de.emaeuer.environment.elements.shape.ParticleShape;
import de.emaeuer.environment.elements.shape.ShapeEntity;

import java.util.List;

public class Particle extends AbstractElement {

    private double radius;

    public Particle() {
        super(new ParticleShape());
    }

    public double getRadius() {
        return getSize().getX();
    }

    public void setRadius(double radius) {
       getSize().setX(radius);
       getSize().setY(radius);
    }


    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((ParticleShape) this.getShape()).getShapesForElement(this);
    }
}
