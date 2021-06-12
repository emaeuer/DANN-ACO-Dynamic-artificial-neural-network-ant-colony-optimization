package de.emaeuer.environment.elements;

import de.emaeuer.environment.elements.shape.Shape;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import de.emaeuer.environment.force.Force;
import de.emaeuer.environment.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElement {

    private double mass;
    private double maxVelocity;

    private final Shape<? extends AbstractElement> shape;

    private final Vector2D size = new Vector2D();
    private final Vector2D acceleration = new Vector2D();
    private final Vector2D velocity = new Vector2D();
    private final Vector2D position = new Vector2D();

    private String color;
    private String borderColor;

    private final List<Force> permanentForces = new ArrayList<>();

    protected AbstractElement(Shape<? extends AbstractElement> shape) {
        this.shape = shape;
    }

    public void step() {
        // apply all permanent forces
        this.permanentForces.forEach(force -> force.accept(this));
        this.velocity.add(this.acceleration);
        this.acceleration.multiply(0);
        this.velocity.limit(this.maxVelocity);
        this.position.add(this.velocity);
    }

    public void addPermanentForce(Force force) {
        this.permanentForces.add(force);
    }

    public void applyForce(Vector2D force) {
        this.acceleration.add(new Vector2D(force).divide(this.mass));
    }

    public void clearForces() {
        this.permanentForces.clear();
    }

    public double getMaxVelocity() {
        return maxVelocity;
    }

    public void setMaxVelocity(double maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public Vector2D getAcceleration() {
        return acceleration;
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public Vector2D getPosition() {
        return position;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBorderColor() {
        return this.borderColor;
    }

    public void setBorderColor(String color) {
        this.borderColor = color;
    }

    protected Shape<? extends AbstractElement> getShape() {
        return this.shape;
    }

    public abstract List<ShapeEntity> getShapesOfElement();

    public Vector2D getSize() {
        return size;
    }

}
