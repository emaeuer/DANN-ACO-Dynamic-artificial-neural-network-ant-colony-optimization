package de.emaeuer.state.value;

import java.util.*;

public class ScatteredDataStateValue extends AbstractStateValue<Map.Entry<Integer, Double[]>, Map<Integer, Double[]>> {

    private final Map<Integer, Double[]> iterationScatteredData = new HashMap<>();
    private final Set<Integer> indicesToRefresh = new HashSet<>();

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
    protected void handleNewValue(Map.Entry<Integer, Double[]> value) {
        if (value != null) {
            this.iterationScatteredData.put(value.getKey(), value.getValue());
            this.indicesToRefresh.add(value.getKey());
        }
    }

    @Override
    protected Map<Integer, Double[]> getValueImpl() {
        return this.iterationScatteredData;
    }

    public Set<Integer> getIndicesToRefresh() {
        return this.indicesToRefresh;
    }
}
