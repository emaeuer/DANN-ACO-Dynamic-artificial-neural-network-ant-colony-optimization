package de.emaeuer.variation;

import java.util.Iterator;

public abstract class VariationParameter<T> implements Iterator<VariationParameter.StaticParameter<T>>, Iterable<VariationParameter.StaticParameter<T>> {

    public static record StaticParameter<T>(String name, T value) {
        @Override
        public String toString() {
            if (value instanceof Double number) {
                return String.format("[%s = %.4f]", name, number);
            } else {
                return String.format("[%s = %s]", name, value);
            }
        }
    }

    private final String name;

    protected VariationParameter(String name) {
        this.name = name;
    }

    public abstract StaticParameter<T> reset();

    @Override
    public Iterator<StaticParameter<T>> iterator() {
        return this;
    }

    public String getName() {
        return name;
    }

    public abstract String toString();
}
