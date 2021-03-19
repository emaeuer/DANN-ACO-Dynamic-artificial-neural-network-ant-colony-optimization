package de.emaeuer.state.value;

import de.emaeuer.state.StateHandler;

public class EmbeddedState extends AbstractStateValue<StateHandler<?>, StateHandler<?>> {

    private StateHandler<?> value;

    @Override
    public Class<? extends StateHandler<?>> getExpectedInputType() {
        Class<?> type = StateHandler.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends StateHandler<?>>) type;
    }

    @Override
    public Class<? extends StateHandler<?>> getOutputType() {
        Class<?> type = StateHandler.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends StateHandler<?>>) type;
    }

    @Override
    protected void handleNewValue(StateHandler<?> value) {
        this.value = value;
    }

    @Override
    public StateHandler<?> getValueImpl() {
        return this.value;
    }
}
