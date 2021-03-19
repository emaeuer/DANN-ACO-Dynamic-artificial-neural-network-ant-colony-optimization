package de.emaeuer.state.value;

public class NumberStateValue extends AbstractStateValue<Number, Number> {

    private Number value = 0;

    @Override
    public Class<? extends Number> getExpectedInputType() {
        return Number.class;
    }

    @Override
    public Class<? extends Number> getOutputType() {
        return Number.class;
    }

    @Override
    protected void handleNewValue(Number value) {
        this.value = value;
    }

    @Override
    public Number getValueImpl() {
        return this.value;
    }
}
