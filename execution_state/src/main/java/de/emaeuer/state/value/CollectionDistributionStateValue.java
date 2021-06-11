package de.emaeuer.state.value;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionDistributionStateValue extends AbstractStateValue<List<Double>, List<List<Double>>> {

    private final DataSeriesStateValue representation = new DataSeriesStateValue();

    private List<List<Double>> values = new ArrayList<>();

    private double overallMax = Double.MIN_VALUE;
    private double dataCount = 0;

    @Override
    public Class<? extends List<Double>> getExpectedInputType() {
        Class<?> type = List.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends List<Double>>) type;
    }

    @Override
    public Class<? extends List<List<Double>>> getOutputType() {
        Class<?> type = List.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends List<List<Double>>>) type;
    }

    @Override
    protected String handleNewValue(List<Double> value) {
        if (value == null) {
            value = Collections.emptyList();
        }

        this.values.add(value);

        updateRepresentation(value);

        return value.stream()
                .map(d -> Double.toString(d))
                .collect(Collectors.joining(","));
    }

    private void updateRepresentation(List<Double> value) {
        DoubleSummaryStatistics statistics = value.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        this.dataCount += value.size();
        this.overallMax = Double.max(this.overallMax, statistics.getMax());

        this.representation.newValue(new AbstractMap.SimpleEntry<>("Min", new Double[] {this.dataCount, statistics.getMin()}));
        this.representation.newValue(new AbstractMap.SimpleEntry<>("Max", new Double[] {this.dataCount, statistics.getMax()}));
        this.representation.newValue(new AbstractMap.SimpleEntry<>("Average", new Double[] {this.dataCount, statistics.getAverage()}));
        this.representation.newValue(new AbstractMap.SimpleEntry<>("Overall max", new Double[] {this.dataCount, this.overallMax}));
    }

    @Override
    protected List<List<Double>> getValueImpl() {
        return this.values;
    }

    @Override
    public String getExportValue() {
        return this.values.stream()
                .map(v -> v.stream().map(Objects::toString).collect(Collectors.joining(",")))
                .map(v -> "[" + v + "]")
                .collect(Collectors.joining(","));
    }

    public AbstractStateValue<?,?> getAlternativeRepresentation() {
        return this.representation;
    }

}
