package de.uni.math;

import org.apache.commons.math3.linear.RealVector;

import java.util.Random;

public class MathUtil {

    private MathUtil() {}

    public static int selectRandomElementFromVector(RealVector vector) {
        double sum = vector.getL1Norm(); // Sum of abs is suitable because pheromone value should always be positive
        double selectionValue = Math.random() * sum;
        double cumulatedSum = 0;

        for (int i = 0; i < vector.getDimension(); i++) {
            cumulatedSum += vector.getEntry(i);
            if (cumulatedSum >= selectionValue) {
                return i;
            }
        }

        throw new IllegalArgumentException("Failed to select a random element from the vector " + vector.toString());
    }

    public static int selectRandomElementFromVector(RealVector vector, boolean invertedProbabilities) {
        if (invertedProbabilities) {
            vector = vector.map(v -> 1 - v);
        }

        return selectRandomElementFromVector(vector);
    }

}
