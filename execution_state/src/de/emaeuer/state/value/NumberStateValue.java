package de.emaeuer.state.value;

import java.util.Objects;

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
    protected String handleNewValue(Number value) {
        this.value = value;
        return value.toString();
    }

    @Override
    public Number getValueImpl() {
        return this.value;
    }

    @Override
    public String getExportValue() {
        return Objects.requireNonNullElse(value, 0).toString();
    }
}
