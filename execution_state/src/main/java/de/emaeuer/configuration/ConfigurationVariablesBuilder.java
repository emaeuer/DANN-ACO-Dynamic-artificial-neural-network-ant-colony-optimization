package de.emaeuer.configuration;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationVariablesBuilder<S extends Enum<S> & ConfigurationVariable> {

    private final Map<String, Double> variables = new HashMap<>();

    private ConfigurationVariablesBuilder() {}

    public static <S extends Enum<S> & ConfigurationVariable> ConfigurationVariablesBuilder<S> build() {
        return new ConfigurationVariablesBuilder<>();
    }

    public ConfigurationVariablesBuilder<S> with(S key, double value) {
        this.variables.put(key.getEquationAbbreviation(), value);
        return this;
    }

    public Map<String, Double> getVariables() {
        return this.variables;
    }
}
