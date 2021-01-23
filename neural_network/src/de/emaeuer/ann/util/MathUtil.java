package de.emaeuer.ann.util;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.stream.IntStream;

public class MathUtil {

    private static final int NONE = -1;

    private MathUtil() {}

    public static RealMatrix removeColumnFromMatrix(RealMatrix matrix, int columnIndex) {
        return getSubMatrix(matrix, MathUtil.NONE, columnIndex);
    }

    public static RealMatrix removeRowFromMatrix(RealMatrix matrix, int rowIndex) {
        return getSubMatrix(matrix, rowIndex, MathUtil.NONE);
    }

    private static RealMatrix getSubMatrix(RealMatrix matrix, int rowToRemove, int colToRemove) {
        if (rowToRemove >= matrix.getRowDimension()) {
            throw new IndexOutOfBoundsException(String.format("Can't remove row %s from matrix with only %s rows", rowToRemove, matrix.getRowDimension()));
        } else if (colToRemove >= matrix.getColumnDimension()) {
            throw new IndexOutOfBoundsException(String.format("Can't remove column %s from matrix with only %s columns", colToRemove, matrix.getColumnDimension()));
        }


        int[] selectedRows = IntStream.range(0, matrix.getRowDimension()).filter(i -> i != rowToRemove).toArray();
        int[] selectedCols = IntStream.range(0, matrix.getColumnDimension()).filter(i -> i != colToRemove).toArray();

        return matrix.getSubMatrix(selectedRows, selectedCols);
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
