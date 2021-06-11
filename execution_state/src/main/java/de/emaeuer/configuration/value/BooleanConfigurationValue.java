package de.emaeuer.configuration.value;

import java.io.Serial;
import java.util.Map;

public class BooleanConfigurationValue extends AbstractConfigurationValue<Boolean> {

    @Serial
    private static final long serialVersionUID = 5302338981317095576L;

    private boolean value;

    public BooleanConfigurationValue(boolean value) {
        super(Boolean.toString(value));
    }

    @Override
    public void setValue(String value) {
        this.value = Boolean.parseBoolean(value);
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String getStringRepresentation() {
        return Boolean.toString(value);
    }

    @Override
    public Boolean getValueForState(Map<String, Double> variables) {
        return this.value;
    }

    @Override
    public AbstractConfigurationValue<Boolean> copy() {
        return new BooleanConfigurationValue(this.value);
    }

}
