package de.emaeuer.variation;

public class DoubleVariationParameter extends VariationParameter<Double> {

    private final double minValue;
    private final double maxValue;
    private final double stepSize;

    private double currentValue;

    public DoubleVariationParameter(String name, double startValue, double maxValue, int numberOfValues) {
        super(name);

        if (numberOfValues < 1) {
            throw new IllegalArgumentException("The number of values must be at least one");
        } else if (startValue >= maxValue) {
            throw new IllegalArgumentException("The start value must be lower than the max value");
        }

        this.minValue = startValue;
        this.maxValue = maxValue;
        this.stepSize = (maxValue - startValue) / (numberOfValues - 1);
        this.currentValue = startValue;
    }

    @Override
    public boolean hasNext() {
        return this.currentValue <= maxValue;
    }

    @Override
    public StaticParameter<Double> next() {
        double result = this.currentValue;
        this.currentValue += this.stepSize;
        return new StaticParameter<>(getName(), result);
    }

    @Override
    public StaticParameter<Double> reset() {
        this.currentValue = this.minValue;
        return next();
    }

    @Override
    public String toString() {
        return getName() + " = " + this.currentValue;
    }
}
