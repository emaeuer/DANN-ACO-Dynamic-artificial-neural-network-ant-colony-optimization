package de.emaeuer.math;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

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

    public static RealMatrix removeColumnFromMatrix(RealMatrix matrix, int columnIndex) {
        RealMatrix newMatrix = MatrixUtils.createRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension() - 1);
        for (int i = 0; i < matrix.getColumnDimension(); i++) {
            if (i == columnIndex) {
                continue;
            }
            newMatrix.setColumnVector(i, matrix.getColumnVector(i));
        }
        return newMatrix;
    }

    public static RealMatrix removeRowFromMatrix(RealMatrix matrix, int rowIndex) {
        RealMatrix newMatrix = MatrixUtils.createRealMatrix(matrix.getRowDimension() - 1, matrix.getColumnDimension());
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            if (i == rowIndex) {
                continue;
            }
            newMatrix.setRowVector(i, matrix.getRowVector(i));
        }
        return newMatrix;
    }

    public static RealVector removeElementFromVector(RealVector vector, int index) {
        return vector.getSubVector(0, index)
                .append(vector.getSubVector(index + 1, vector.getDimension() - index - 1));
    }

    public static RealMatrix addColumnToMatrix(RealMatrix matrix) {
        RealMatrix newMatrix = MatrixUtils.createRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension() + 1);

        for (int i = 0; i < matrix.getColumnDimension(); i++) {
            newMatrix.setColumnVector(i, matrix.getColumnVector(i));
        }

        return newMatrix;
    }

    public static RealMatrix addRowToMatrix(RealMatrix matrix) {
        RealMatrix newMatrix = MatrixUtils.createRealMatrix(matrix.getRowDimension() + 1, matrix.getColumnDimension());

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            newMatrix.setRowVector(i, matrix.getRowVector(i));
        }

        return newMatrix;
    }

    public static RealVector addElementToVector(RealVector vector) {
        return vector.append(0);
    }
}
