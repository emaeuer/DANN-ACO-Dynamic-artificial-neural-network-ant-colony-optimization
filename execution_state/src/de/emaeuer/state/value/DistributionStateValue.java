package de.emaeuer.state.value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

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
    protected void handleNewValue(Double value) {
        if (value == null) {
            value = 0.0;
        }

        this.values.add(value);
        this.sum += value;
        this.sumOfSquares += Math.pow(value, 2);
    }

    @Override
    protected List<Double> getValueImpl() {
        return this.values;
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
