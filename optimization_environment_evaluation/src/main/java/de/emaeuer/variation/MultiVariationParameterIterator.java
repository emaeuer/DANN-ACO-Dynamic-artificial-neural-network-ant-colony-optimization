package de.emaeuer.variation;

import de.emaeuer.variation.VariationParameter.StaticParameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultiVariationParameterIterator implements Iterator<List<StaticParameter<?>>>, Iterable<List<StaticParameter<?>>> {

    private final List<VariationParameter<?>> parameters = new ArrayList<>();
    private final List<StaticParameter<?>> currentState = new ArrayList<>();

    private boolean isIterating = false;

    public void addParameter(VariationParameter<?> parameter) {
        if (this.isIterating) {
            throw new IllegalStateException("Iteration is currently in progress");
        }

        this.currentState.add(parameter.next());
        this.parameters.add(parameter);
    }

    @Override
    public boolean hasNext() {
        return !this.currentState.isEmpty() ;
    }

    @Override
    public List<StaticParameter<?>> next() {
        this.isIterating = true;

        List<StaticParameter<?>> result = new ArrayList<>(this.currentState);

        boolean hasNext = this.parameters.stream().anyMatch(VariationParameter::hasNext);
        if (!hasNext) {
            this.currentState.clear();
            return result;
        }

        for (int i = 0; i < this.parameters.size(); i++) {
            VariationParameter<?> parameter = this.parameters.get(i);

            if (parameter.hasNext()) {
                this.currentState.set(i, parameter.next());
                break;
            } else {
                this.currentState.set(i, parameter.reset());
            }
        }

        return result;
    }

    @Override
    public Iterator<List<StaticParameter<?>>> iterator() {
        return this;
    }
}
