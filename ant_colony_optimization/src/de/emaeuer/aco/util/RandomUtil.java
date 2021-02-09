package de.emaeuer.aco.util;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.RealVector;

public class RandomUtil {

    private RandomUtil() {}

    public static int selectRandomElementFromVector(RealVector vector) {
        double sum = vector.getL1Norm(); // Sum of abs is suitable because pheromone value should always be positive
        double selectionValue = Math.random() * sum;
        double cumulatedSum = 0;

        for (int i = 0; i < vector.getDimension(); i++) {
            cumulatedSum += vector.getEntry(i);
            if (cumulatedSum > selectionValue) {
                return i;
            }
        }

        // rare case that Math.random() was 1 or vector only contains one element
        if (cumulatedSum == selectionValue) {
            return vector.getDimension() - 1;
        }

        throw new IllegalArgumentException("Failed to select a random element from the vector " + vector.toString());
    }

    public static int selectRandomElementFromVector(RealVector vector, boolean invertedProbabilities) {
        if (invertedProbabilities) {
            double sum = vector.getL1Norm();
            vector = vector.map(v -> v == 0 ? 0 : 1 - (v / sum)); // 0 stays 0
        }

        return selectRandomElementFromVector(vector);
    }

    public static double getNormalDistributedValue(double mean, double deviation) {
        return new NormalDistribution(mean, deviation).sample();
    }

}
