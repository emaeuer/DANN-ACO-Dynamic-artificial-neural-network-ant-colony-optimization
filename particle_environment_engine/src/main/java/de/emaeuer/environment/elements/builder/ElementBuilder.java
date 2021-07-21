package de.emaeuer.environment.elements.builder;

import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.force.Force;
import de.emaeuer.environment.math.Vector2D;

import java.util.Arrays;
import java.util.function.Consumer;

public abstract class ElementBuilder<T extends AbstractElement, S extends ElementBuilder<T, S>> {

    private final T element;

    public ElementBuilder() {
        this.element = getElementImplementation();
        mass(1);
        maxVelocity(20);
    }

    protected abstract T getElementImplementation();
    protected abstract S getThis();

    public S mass(double mass) {
        this.element.setMass(mass);
        return getThis();
    }

    public S size(Vector2D size) {
        Vector2D elementSize = this.element.getSize();
        elementSize.setX(size.getX());
        elementSize.setY(size.getY());
        return getThis();
    }

    public S maxVelocity(double maxVelocity) {
        this.element.setMaxVelocity(maxVelocity);
        return getThis();
    }

    public S addPermanentForce(Force... force) {
        Arrays.stream(force).forEach(this.element::addPermanentForce);
        return getThis();
    }

    public S setStartPosition(double x, double y) {
        Vector2D position = this.element.getPosition();
        position.setX(x);
        position.setY(y);
        return getThis();
    }

    public S initialImpulse(Consumer<AbstractElement> impulse) {
        impulse.accept(this.element);
        return getThis();
    }

    public T build() {
        return this.element;
    }

    protected T getElement() {
        return this.element;
    }
}
