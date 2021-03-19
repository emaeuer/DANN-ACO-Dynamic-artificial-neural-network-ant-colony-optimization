package de.emaeuer.configuration.value;

import java.io.Serial;
import java.util.Map;

public class DoubleConfigurationValue extends AbstractConfigurationValue<Double> {

    @Serial
    private static final long serialVersionUID = 5302338981317095576L;

    private double value;

    public DoubleConfigurationValue(double value) {
        super(Double.toString(value));
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

}
