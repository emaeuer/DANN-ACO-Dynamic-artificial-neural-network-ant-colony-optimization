package de.emaeuer.state.value;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ScatteredDataStateValue extends AbstractStateValue<Map.Entry<Integer, Double[]>, Map<Integer, Double[]>> {

    private final Map<Integer, Double[]> iterationScatteredData = new ConcurrentHashMap<>();
    private final Set<Integer> indicesToRefresh = ConcurrentHashMap.newKeySet();

    private Double[] valuesToExport;

    @Override
    public Class<? extends Map.Entry<Integer, Double[]>> getExpectedInputType() {
        Class<?> type = Map.Entry.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map.Entry<Integer, Double[]>>) type;
    }

    @Override
    public Class<? extends Map<Integer, Double[]>> getOutputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<Integer, Double[]>>) type;
    }

    @Override
    protected String handleNewValue(Map.Entry<Integer, Double[]> value) {
        if (value != null) {
            this.iterationScatteredData.put(value.getKey(), value.getValue());
            this.indicesToRefresh.add(value.getKey());
            this.valuesToExport = value.getValue();
        }
        return null;
    }

    @Override
    protected Map<Integer, Double[]> getValueImpl() {
        return this.iterationScatteredData;
    }

    @Override
    public String getExportValue() {
        return Arrays.stream(this.valuesToExport)
                .map(Objects::toString)
                .collect(Collectors.joining(","));
    }

    public Set<Integer> getIndicesToRefresh() {
        return this.indicesToRefresh;
    }

}