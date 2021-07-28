package de.emaeuer.variation;

public class IntegerVariationValue extends VariationParameter<Integer> {

    private final int minValue;
    private final int maxValue;
    private final int stepSize;

    private int currentValue;

    public IntegerVariationValue(String name, int startValue, int maxValue, int stepSize) {
        super(name);

        if (stepSize < 1) {
            throw new IllegalArgumentException("The step size must be greater than 0");
        } else if (startValue >= maxValue) {
            throw new IllegalArgumentException("The start value must be lower than the max value");
        }

        this.minValue = startValue;
        this.maxValue = maxValue;
        this.stepSize = stepSize;
        this.currentValue = startValue;
    }

    @Override
    public boolean hasNext() {
        return this.currentValue <= maxValue;
    }

    @Override
    public StaticParameter<Integer> next() {
        int result = this.currentValue;
        this.currentValue += this.stepSize;
        return new StaticParameter<>(getName(), result);
    }

    @Override
    public StaticParameter<Integer> reset() {
        this.currentValue = minValue;
        return next();
    }

    @Override
    public String toString() {
        return getName() + " = " + this.currentValue;
    }
}
