package de.emaeuer.state.value;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractStateValue<I, O> {

    private boolean changedSinceLastGet = true;

    public abstract Class<? extends I> getExpectedInputType();
    public abstract Class<? extends O> getOutputType();

    public void newValue(Object value) {
        if (!getExpectedInputType().isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected value of type %s received one of %s instead", getExpectedInputType().getSimpleName(), value.getClass().getSimpleName()));
        }

        this.changedSinceLastGet = true;

        //noinspection unchecked no safe way to cast generics but was checked
        handleNewValue((I) value);
    }

    protected abstract void handleNewValue(I value);

    public O getValue() {
        this.changedSinceLastGet = false;
        return getValueImpl();
    }

    protected abstract O getValueImpl();

    public boolean changedSinceLastGet() {
        return changedSinceLastGet;
    }

}
