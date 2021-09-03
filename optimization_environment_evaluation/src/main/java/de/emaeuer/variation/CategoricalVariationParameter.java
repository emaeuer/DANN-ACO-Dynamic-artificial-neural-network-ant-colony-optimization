package de.emaeuer.variation;

import java.util.Collection;
import java.util.Iterator;

public class CategoricalVariationParameter extends VariationParameter<String> {

    private final Collection<String> values;
    private Iterator<String> iterator;

    private String currentValue;

    public CategoricalVariationParameter(String name, Collection<String> values) {
        super(name);
        this.values = values;
        this.iterator = values.iterator();
        currentValue = this.iterator.next();
        this.iterator = values.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public StaticParameter<String> next() {
        this.currentValue = iterator.next();
        return new StaticParameter<>(getName(), this.currentValue);
    }

    @Override
    public StaticParameter<String> reset() {
        this.iterator = values.iterator();
        return iterator().next();
    }

    @Override
    public String toString() {
        return String.format("[%s = %s]", getName(), currentValue);
    }
}
