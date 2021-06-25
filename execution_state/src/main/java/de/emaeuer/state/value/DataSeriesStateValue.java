package de.emaeuer.state.value;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataSeriesStateValue extends AbstractStateValue<Map.Entry<String, Double[]>, Map<String, List<Double[]>>> {

    private final Map<String, List<Double[]>> seriesData = new ConcurrentHashMap<>();

    private final Map<Double, Integer> existingXValues = new ConcurrentHashMap<>();
    private final Set<Integer> indicesToRefresh = ConcurrentHashMap.newKeySet();

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
    protected String handleNewValue(Map.Entry<String, Double[]> value) {
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
        return null;
    }

    private int addNewData(Map.Entry<String, Double[]> value) {
        int indexToRefresh;
        this.seriesData.putIfAbsent(value.getKey(), Collections.synchronizedList(new ArrayList<>()));
        this.seriesData.get(value.getKey()).add(value.getValue());
        indexToRefresh = this.seriesData.get(value.getKey()).size() - 1;
        existingXValues.put(value.getValue()[0], indexToRefresh);
        return indexToRefresh;
    }

    @Override
    public Map<String, List<Double[]>> getValueImpl() {
        return this.seriesData;
    }

    @Override
    public String getExportValue() {
        return this.seriesData.toString();
    }

    public Set<Integer> getIndicesToRefresh() {
        return indicesToRefresh;
    }
}