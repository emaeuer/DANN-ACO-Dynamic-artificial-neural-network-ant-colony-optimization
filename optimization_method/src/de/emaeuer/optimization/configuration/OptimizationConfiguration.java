package de.emaeuer.optimization.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class OptimizationConfiguration<T extends ConfigurationKey, S extends OptimizationParameterNames> {

    private final Map<T, ConfigurationValue<T>> configurations = new HashMap<>();

    public OptimizationConfiguration(List<ConfigurationValue<T>> configurations) {
        getConfigurations().putAll(getDefaultConfiguration());
        // overwrite default values
        configurations
                .forEach(c -> getConfigurations().put(c.getKey(), c));

    }

    protected abstract Map<T, ConfigurationValue<T>> getDefaultConfiguration();

    public void setValue(ConfigurationValue<T> value) {
        this.configurations.put(value.getKey(), value);
    }

    public double getValue(T key, OptimizationParameter<S> parameters) {
        return this.configurations.get(key).apply(parameters);
    }

    public double getValue(T key) {
        return this.configurations.get(key).apply(null);
    }

    public int getValueAsInt(T key) {
        return Double.valueOf(getValue(key)).intValue();
    }

    protected Map<T, ConfigurationValue<T>> getConfigurations() {
        return this.configurations;
    }
}