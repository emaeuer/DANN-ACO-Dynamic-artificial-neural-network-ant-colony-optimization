package de.emaeuer.state.value;

import java.util.*;

public class DataSeriesStateValue extends AbstractStateValue<Map.Entry<String, Double[]>, Map<String, List<Double[]>>> {

    private final Map<String, List<Double[]>> seriesData = new HashMap<>();

    private final Map<Double, Integer> existingXValues = new HashMap<>();
    private final Set<Integer> indicesToRefresh = new HashSet<>();

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
        Double[] point = value.getValue();
        int indexToRefresh;

        if (!existingXValues.containsKey(point[0])) {
            indexToRefresh = addNewData(value);
        } else {
            indexToRefresh = this.existingXValues.get(point[0]);
            if (!this.seriesData.containsKey(value.getKey()) || indexToRefresh == this.seriesData.get(value.getKey()).size()) {
                addNewData(value);
            } else {
                this.seriesData.get(value.getKey()).set(indexToRefresh, point);
            }
        }

        this.indicesToRefresh.add(indexToRefresh);
    }

    private int addNewData(Map.Entry<String, Double[]> value) {
        int indexToRefresh;
        this.seriesData.putIfAbsent(value.getKey(), new ArrayList<>());
        this.seriesData.get(value.getKey()).add(value.getValue());
        indexToRefresh = this.seriesData.get(value.getKey()).size() - 1;
        existingXValues.put(value.getValue()[0], indexToRefresh);
        return indexToRefresh;
    }

    @Override
    public Map<String, List<Double[]>> getValueImpl() {
        return this.seriesData;
    }

    public Set<Integer> getIndicesToRefresh() {
        return indicesToRefresh;
    }
}
