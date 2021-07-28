package de.emaeuer.state.value;

import de.emaeuer.state.value.data.DataPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataQuantityStateValue extends AbstractStateValue<Map<String, Long>, Map<String, Long>> {

    private final Map<String, Long> quantities = new HashMap<>();

    @Override
    public Class<? extends Map<String, Long>> getExpectedInputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<String, Long>>) type;
    }

    @Override
    public Class<? extends Map<String, Long>> getOutputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<String, Long>>) type;
    }

    @Override
    protected String handleNewValue(Map<String, Long> value) {
        value.forEach((k, v) -> this.quantities.compute(k, (k1, v1) -> Objects.requireNonNullElse(v1, 0L) + v));
        return value.entrySet()
                .stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    @Override
    protected Map<String, Long> getValueImpl() {
        return this.quantities;
    }

    @Override
    public String getExportValue() {
        double sum = this.quantities.values()
                .stream()
                .mapToLong(l -> l)
                .sum();

        return this.quantities.entrySet()
                .stream()
                .map(e -> String.format("%s:%d (%.2f)", e.getKey(), e.getValue(), e.getValue() / sum))
                .collect(Collectors.joining(", "));
    }
}
