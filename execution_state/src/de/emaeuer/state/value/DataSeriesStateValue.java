package de.emaeuer.state.value;

import java.util.*;

public class DataSeriesStateValue extends AbstractStateValue<Map.Entry<String, Double[]>, Map<String, List<Double[]>>> {

    private final Map<String, List<Double[]>> seriesData = new HashMap<>();

    @Override
    public Class<? extends Map.Entry<String, Double[]>> getExpectedInputType() {
        Class<?> type = Map.Entry.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map.Entry<String, Double[]>>) type;
    }

    @Override
    public Class<? extends Map<String, List<Double[]>>> getOutputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<String, List<Double[]>>>) type;
    }

    @Override
    protected void handleNewValue(Map.Entry<String, Double[]> value) {
        this.seriesData.putIfAbsent(value.getKey(), new ArrayList<>());
        this.seriesData.get(value.getKey()).add(value.getValue());
    }

    @Override
    public Map<String, List<Double[]>> getValueImpl() {
        return this.seriesData;
    }
}
