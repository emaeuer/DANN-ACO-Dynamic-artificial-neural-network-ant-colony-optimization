package de.emaeuer.configuration.value;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

public class NumericListConfigurationValue extends AbstractConfigurationValue<List<Double>> {

    @Serial
    private static final long serialVersionUID = 3966921703271334755L;

    private List<Double> value;

    public NumericListConfigurationValue(String value) {
        super(value);
    }

    @Override
    public void setValue(String value) {
        if (value.isBlank()) {
            this.value = Collections.emptyList();
        } else {
            this.value = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .toList();
        }
    }

    @Override
    public String getStringRepresentation() {
        if (value != null) {
            return this.value.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    @Override
    public List<Double> getValueForState(Map<String, Double> variables) {
        return this.value;
    }

    @Override
    public AbstractConfigurationValue<List<Double>> copy() {
        return new NumericListConfigurationValue(getStringRepresentation());
    }
}
