package de.emaeuer.configuration.value;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StringConfigurationValue extends AbstractConfigurationValue<String> {

    @Serial
    private static final long serialVersionUID = 3230400440533422114L;

    private String value;
    private final List<String> possibleValues = new ArrayList<>();

    public StringConfigurationValue(String value, String... possibleValues) {
        super(value);
        this.possibleValues.addAll(Arrays.asList(possibleValues));
    }

    @Override
    public void setValue(String value) {
        if (possibleValues == null || possibleValues.isEmpty() || possibleValues.contains(value)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Value is not one of the possible values");
        }
    }

    @Override
    public String getStringRepresentation() {
        return this.value;
    }

    @Override
    public String getValueForState(Map<String, Double> variables) {
        return this.value;
    }

    @Override
    public AbstractConfigurationValue<String> copy() {
        return new StringConfigurationValue(this.value, this.possibleValues.toArray(String[]::new));
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }
}
