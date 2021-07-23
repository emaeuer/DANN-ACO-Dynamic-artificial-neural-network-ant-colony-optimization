package de.emaeuer.variation;

import java.util.Iterator;

public abstract class VariationParameter<T> implements Iterator<VariationParameter.StaticParameter<T>>, Iterable<VariationParameter.StaticParameter<T>> {

    public static record StaticParameter<T>(String name, T value) {}

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
}
