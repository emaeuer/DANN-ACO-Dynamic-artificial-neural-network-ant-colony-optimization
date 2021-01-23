package de.emaeuer.environment.elements.builder;

import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.force.Force;
import de.emaeuer.math.Vector2D;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

public abstract class ElementBuilder<T extends AbstractElement, S extends ElementBuilder<T, S>> {

    private static final String COLOR_PATTERN = "rgba(%d,%d,%d,%.2f)";

    private final T element;

    public ElementBuilder() {
        this.element = getElementImplementation();
        color(0, 0, 0);
        mass(1);
        maxVelocity(20);
        borderColor(0, 0, 0);
    }

    protected abstract T getElementImplementation();
    protected abstract S getThis();

    public S color(int red, int green, int blue) {
        return color(red, green, blue, 1);
    }

    public S color(int red, int green, int blue, double alpha) {
        this.element.setColor(String.format(Locale.ROOT, COLOR_PATTERN, red, green, blue, alpha));
        return getThis();
    }

    public S borderColor(int red, int green, int blue) {
        return borderColor(red, green, blue, 1);
    }

    public S borderColor(int red, int green, int blue, double alpha) {
        this.element.setBorderColor(String.format(Locale.ROOT, COLOR_PATTERN, red, green, blue, alpha));
        return getThis();
    }

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
