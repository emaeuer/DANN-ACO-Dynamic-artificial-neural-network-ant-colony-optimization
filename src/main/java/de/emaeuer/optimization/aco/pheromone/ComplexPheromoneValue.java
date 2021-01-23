package de.emaeuer.optimization.aco.pheromone;

public class ComplexPheromoneValue {

    private static final double DISSIPATIVE_FACTOR = 0.1;

    private static final double MAX_VALUE = 1;
    private static final double MIN_VALUE = -1;

    private static final double INITIAL_VALUE = 0;
    private static final double INITIAL_PHEROMONE_VALUE = 0.1;

    // value in [MIN_VALUE:0) = inhibitory; value in (0:MAX_VALUE] = excitatory
    private double value = INITIAL_VALUE;
    private double fixed = INITIAL_PHEROMONE_VALUE;

    public ComplexPheromoneValue(double initValue) {
        setValue(initValue);
    }

    public void update(double value, double quality) {
        if (quality > 0) {
            setValue(value);
            this.fixed += (1 - this.fixed) / (this.fixed + 2);
        }
    }

    public void dissipate() {
        this.fixed *= 1 - DISSIPATIVE_FACTOR;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        // value is always between MIN_VALUE and MAX_VALUE
        this.value = Math.max(Math.min(value, MAX_VALUE), MIN_VALUE);
    }

    public double getFixed() {
        return fixed;
    }

    public void setFixed(double fixed) {
        this.fixed = fixed;
    }

    public boolean isInhibitory() {
        return this.value < 0;
    }

    public boolean isExcitatory() {
        return this.value > 0;
    }
}
