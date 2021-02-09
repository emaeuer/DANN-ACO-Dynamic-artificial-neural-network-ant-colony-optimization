package de.emaeuer.optimization.configuration;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class OptimizationParameter<T extends OptimizationParameterNames> {

    private final Map<T, Double> parameterValues = new HashMap<>();

    public Map<String, Double> toParameters() {
        return parameterValues.entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().getName(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void setParameterValue(T parameter, double value) {
        this.parameterValues.put(parameter, value);
    }

}
