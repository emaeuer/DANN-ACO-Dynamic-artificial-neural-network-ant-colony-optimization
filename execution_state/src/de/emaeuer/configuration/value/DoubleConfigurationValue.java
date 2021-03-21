package de.emaeuer.configuration.value;

import java.io.Serial;
import java.util.Map;

public class DoubleConfigurationValue extends AbstractConfigurationValue<Double> {

    @Serial
    private static final long serialVersionUID = 5302338981317095576L;

    private double value;
    private final double min;
    private final double max;

    public DoubleConfigurationValue(double value) {
        this(value, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    public DoubleConfigurationValue(double value, double min, double max) {
        super(Double.toString(value));
        this.min = min;
        this.max = max;
    }

    @Override
    public void setValue(String value) {
        this.value = Double.parseDouble(value);
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public String getStringRepresentation() {
        return Double.toString(value);
    }

    @Override
    public Double getValueForState(Map<String, Double> variables) {
        return this.value;
    }

    @Override
    public AbstractConfigurationValue<Double> copy() {
        return new DoubleConfigurationValue(this.value);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }
}
