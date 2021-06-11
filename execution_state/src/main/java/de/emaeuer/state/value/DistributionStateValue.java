package de.emaeuer.state.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DistributionStateValue  extends AbstractStateValue<Double, List<Double>> {

    private List<Double> values = new ArrayList<>();

    private double sum = 0;
    private double sumOfSquares = 0;

    @Override
    public Class<? extends Double> getExpectedInputType() {
        return Double.class;
    }

    @Override
    public Class<? extends List<Double>> getOutputType() {
        Class<?> type = List.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends List<Double>>) type;
    }

    @Override
    protected String handleNewValue(Double value) {
        if (value == null) {
            value = 0.0;
        }

        this.values.add(value);
        this.sum += value;
        this.sumOfSquares += Math.pow(value, 2);

        return Double.toString(value);
    }

    @Override
    protected List<Double> getValueImpl() {
        return this.values;
    }

    @Override
    public String getExportValue() {
        String valuesString = this.values.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(","));
        return String.format("[mean=%f,deviation=%f,values=[%s]]", getMean(), getStandardDeviation(), valuesString);
    }

    public double getMean() {
        if (this.values.isEmpty()) {
            return 0;
        } else {
            return this.sum / this.values.size();
        }
    }

    public double getStandardDeviation() {
        if (this.values.isEmpty()) {
            return 0;
        } else {
            return Math.sqrt(this.sumOfSquares / this.values.size() - Math.pow(getMean(), 2));
        }
    }
}
