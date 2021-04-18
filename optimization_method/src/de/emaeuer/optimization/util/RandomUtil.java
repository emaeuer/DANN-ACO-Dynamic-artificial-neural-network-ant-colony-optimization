package de.emaeuer.optimization.util;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.RealVector;

import java.util.Arrays;
import java.util.Random;

public class RandomUtil {

    private static final Random RNG = new Random();

    private RandomUtil() {}

    public static int selectRandomElementFromVector(int[] vector) {
        int sum = Arrays.stream(vector).sum();
        int selectionValue = RNG.nextInt(sum) + 1;
        int cumulatedSum = 0;

        for (int i = 0; i < vector.length; i++) {
            cumulatedSum += vector[i];
            if (cumulatedSum >= selectionValue) {
                return i;
            }
        }

        throw new IllegalArgumentException("Failed to select a random element from the vector " + Arrays.toString(vector));
    }

    public static int selectRandomElementFromVector(RealVector vector) {
        return selectRandomElementFromVector(vector.toArray());
    }

    public static int selectRandomElementFromVector(double[] vector) {
        double sum = Arrays.stream(vector).sum();
        double selectionValue = RNG.nextDouble() * sum;
        double cumulatedSum = 0;

        for (int i = 0; i < vector.length; i++) {
            cumulatedSum += vector[i];
            if (cumulatedSum >= selectionValue) {
                return i;
            }
        }

        throw new IllegalArgumentException("Failed to select a random element from the vector " + Arrays.toString(vector));
    }

    public static int selectRandomElementFromVector(RealVector vector, boolean invertedProbabilities) {
        if (invertedProbabilities) {
            double sum = vector.getL1Norm();
            vector = vector.map(v -> v == 0
                    ? 0
                    : 1 - ((v / sum) == 1 ? 0 : v / sum)); // 0 stays 0
        }

        return selectRandomElementFromVector(vector);
    }

    public static double getNormalDistributedValue(double mean, double deviation) {
        return new NormalDistribution(mean, deviation).sample();
    }

    public static double getBetaDistributedValue(double mean, double deviation) {
        mean = (mean + 1) / 2;
        double normal = mean * (1 - mean) / Math.pow(deviation, 2);
        double alpha = mean * normal;
        double beta = (1 - mean) * normal;

        return new BetaDistribution(alpha, beta).sample() * 2 - 1;
    }

    public static int getNextInt(int min, int max) {
        return RNG.nextInt(max - min) + min;
    }

    public static double getNextDouble(double min, double max) {
        return RNG.nextDouble() * (max - min) + min;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(getBetaDistributedValue(-0.99, 0.04));
        }
    }
}
