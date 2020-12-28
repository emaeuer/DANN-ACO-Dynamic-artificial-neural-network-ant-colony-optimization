package de.uni.environment.elements;

public class Particle extends AbstractElement {

    private double radius;

    public Particle() {
        super(Form.PARTICLE);
    }

    public double getRadius() {
        return getSize().getX();
    }

    public void setRadius(double radius) {
       getSize().setX(radius);
       getSize().setY(radius);
    }


}
