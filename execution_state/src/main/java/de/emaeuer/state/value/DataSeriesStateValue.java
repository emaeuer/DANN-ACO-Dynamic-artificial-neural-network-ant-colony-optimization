package de.emaeuer.state.value;

import de.emaeuer.state.value.data.DataPoint;

import java.util.*;
import java.util.stream.Collectors;

public class DataSeriesStateValue extends AbstractStateValue<Map<String, DataPoint>, Map<String, Map<Double, DataPoint>>> {

    private final Map<String, Map<Double, DataPoint>> seriesData = new HashMap<>();
    private Map<String, List<DataPoint>> newData = new HashMap<>();

    @Override
    public Class<? extends Map<String, DataPoint>> getExpectedInputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<String, DataPoint>>) type;
    }

    @Override
    public Class<? extends Map<String, Map<Double, DataPoint>>> getOutputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<String, Map<Double, DataPoint>>>) type;
    }

    @Override
    protected String handleNewValue(Map<String, DataPoint> value) {
        value.entrySet().forEach(this::addNewDataPoint);

        return value.entrySet()
                .stream()
                .map(e -> String.format("%s=(%f:%f)", e.getKey(), e.getValue().getX(), e.getValue().getY()))
                .collect(Collectors.joining(","));
    }

    protected void addNewDataPoint(Map.Entry<String, DataPoint> value) {
        DataPoint point = value.getValue();
        Map<Double, DataPoint> series = this.seriesData.computeIfAbsent(value.getKey(), k -> new HashMap<>());

        if (!series.containsKey(point.getX())) {
            point.setIndex(series.size());
        } else {
            DataPoint oldPoint = series.get(point.getX());
            point.setIndex(oldPoint.getIndex());
        }

        series.put(point.getX(), point);
        this.newData.putIfAbsent(value.getKey(), new ArrayList<>());
        this.newData.get(value.getKey()).add(point);
    }

    @Override
    public Map<String, Map<Double, DataPoint>> getValueImpl() {
        return this.seriesData;
    }

    @Override
    public String getExportValue() {
        return newData.entrySet()
                .stream()
                .map(e -> {
                    String valueString = e.getValue()
                            .stream()
                            .map(p -> String.format("(%f,%f)", p.getX(), p.getY()))
                            .collect(Collectors.joining(","));
                    return String.format("%s={%s}", e.getKey(), valueString);
                })
                .collect(Collectors.joining(","));
    }

    public Map<String, List<DataPoint>> getChangedData() {
        Map<String, List<DataPoint>> result = this.newData;
        this.newData = new HashMap<>();

        return result;
    }

    public int getSeriesSize(String seriesName) {
        return this.seriesData.getOrDefault(seriesName, Collections.emptyMap()).size();
    }
}
