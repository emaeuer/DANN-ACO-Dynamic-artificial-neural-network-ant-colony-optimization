package de.emaeuer.optimization.util;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RandomUtil {

    private final int seed;
    private final JDKRandomGenerator rng;

    public RandomUtil(int seed) {
        this.seed = seed;
        this.rng = new JDKRandomGenerator(seed);
    }

    public int selectRandomElementFromVector(int[] vector) {
        int sum = Arrays.stream(vector).sum();
        int selectionValue = this.rng.nextInt(sum) + 1;
        int cumulatedSum = 0;

        for (int i = 0; i < vector.length; i++) {
            cumulatedSum += vector[i];
            if (cumulatedSum >= selectionValue) {
                return i;
            }
        }

        throw new IllegalArgumentException("Failed to select a random element from the vector " + Arrays.toString(vector));
    }

    public int selectRandomElementFromVector(RealVector vector) {
        return selectRandomElementFromVector(vector.toArray());
    }

    public int selectRandomElementFromVector(double[] vector) {
        double sum = Arrays.stream(vector).sum();
        double selectionValue = this.rng.nextDouble() * sum;
        double cumulatedSum = 0;

        for (int i = 0; i < vector.length; i++) {
            cumulatedSum += vector[i];
            if (cumulatedSum >= selectionValue) {
                return i;
            }
        }

        throw new IllegalArgumentException("Failed to select a random element from the vector " + Arrays.toString(vector));
    }

    public int selectRandomElementFromVector(RealVector vector, boolean invertedProbabilities) {
        if (invertedProbabilities) {
            double sum = vector.getL1Norm();
            vector = vector.map(v -> v == 0
                    ? 0
                    : 1 - ((v / sum) == 1 ? 0 : v / sum)); // 0 stays 0
        }

        return selectRandomElementFromVector(vector);
    }

    public double getNormalDistributedValue(double mean, double deviation) {
        if (deviation <= 0) {
            return mean;
        }
        return new NormalDistribution(this.rng, mean, deviation).sample();
    }

    public int getNextInt() {
        return this.rng.nextInt();
    }

    public int getNextInt(int min, int max) {
        return this.rng.nextInt(max - min) + min;
    }

    public double getNextDouble(double min, double max) {
        return this.rng.nextDouble() * (max - min) + min;
    }

    public double nextDouble() {
        return this.rng.nextDouble();
    }

    public void reset() {
        this.rng.setSeed(this.seed);
    }

    public void reset(int seed) {
        this.rng.setSeed(seed);
    }

    public void shuffleCollection(List<?> shuffledInput) {
        Collections.shuffle(shuffledInput, this.rng);
    }

}
