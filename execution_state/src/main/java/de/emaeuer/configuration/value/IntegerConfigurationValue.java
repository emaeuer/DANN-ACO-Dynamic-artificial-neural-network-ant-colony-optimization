package de.emaeuer.configuration.value;

import java.io.Serial;
import java.util.Map;

public class IntegerConfigurationValue extends AbstractConfigurationValue<Integer> {

    @Serial
    private static final long serialVersionUID = 5302338981317095576L;

    private int value;
    private final int min;
    private final int max;

    public IntegerConfigurationValue(int value, int min, int max) {
        super(Integer.toString(value));
        this.min = min;
        this.max = max;
    }

    public IntegerConfigurationValue(int value) {
        this(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public void setValue(String value) {
        this.value = Integer.parseInt(value);
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String getStringRepresentation() {
        return Integer.toString(value);
    }

    @Override
    public Integer getValueForState(Map<String, Double> variables) {
        return this.value;
    }

    @Override
    public AbstractConfigurationValue<Integer> copy() {
        return new IntegerConfigurationValue(this.value, this.min, this.max);
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }
}
