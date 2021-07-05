package de.emaeuer.configuration.value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractConfigurationValue<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -2302558258661491611L;

    private boolean disabled = false;

    public AbstractConfigurationValue(String value) {
        setValue(value);
    }

    public abstract void setValue(String value);

    public abstract String getStringRepresentation();

    public abstract T getValueForState(Map<String, Double> variables);

    public abstract AbstractConfigurationValue<T> copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractConfigurationValue<?> that = (AbstractConfigurationValue<?>) o;

        return Objects.equals(getStringRepresentation(), that.getStringRepresentation());
    }

    @Override
    public int hashCode() {
        String stringRepresentation = getStringRepresentation();
        return stringRepresentation != null ? stringRepresentation.hashCode() : 0;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
