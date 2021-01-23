package de.emaeuer.util;

import de.emaeuer.ann.util.MathUtil;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathUtilTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private RealMatrix createFilledMatrix(int row, int column) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(row, column);
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                matrix.setEntry(i, j, Integer.parseInt(Integer.toString(i) + j));
            }
        }
        return matrix;
    }

    private RealVector createFilledVector(int length) {
        RealVector vector = new ArrayRealVector(length);
        IntStream.range(0, length).forEach(i -> vector.setEntry(i, i));
        return vector;
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testRemoveColumnFromMatrix() {
        RealMatrix matrix = createFilledMatrix(4, 3);

        // test remove first column
        RealMatrix resultMatrix = MathUtil.removeColumnFromMatrix(matrix, 0);
        RealMatrix expectedMatrix = matrix.getSubMatrix(0, 3, 1, 2);
        assertEquals(expectedMatrix, resultMatrix);

        // test remove last column
        resultMatrix = MathUtil.removeColumnFromMatrix(matrix, 2);
        expectedMatrix = matrix.getSubMatrix(0, 3, 0, 1);
        assertEquals(expectedMatrix, resultMatrix);

        // test remove middle column
        resultMatrix = MathUtil.removeColumnFromMatrix(matrix, 1);
        expectedMatrix = matrix.getSubMatrix(new int[] {0, 1, 2, 3}, new int[] {0, 2});
        assertEquals(expectedMatrix, resultMatrix);
    }

    @Test
    public void testAddColumnToMatrix() {
        RealMatrix matrix = createFilledMatrix(2, 2);
        RealMatrix resultMatrix = MathUtil.addColumnToMatrix(matrix);

        // check matrices are equal when the added column is removed again
        assertEquals(matrix, MathUtil.removeColumnFromMatrix(resultMatrix, 2));
        // check appended column is filled with 0
        assertArrayEquals(new double[] {0, 0}, resultMatrix.getColumn(2));
    }

    @Test
    public void testRemoveRowFromMatrix() {
        RealMatrix matrix = createFilledMatrix(3, 4);

        // test remove first row
        RealMatrix resultMatrix = MathUtil.removeRowFromMatrix(matrix, 0);
        RealMatrix expectedMatrix = matrix.getSubMatrix(1, 2, 0, 3);
        assertEquals(expectedMatrix, resultMatrix);

        // test remove last row
        resultMatrix = MathUtil.removeRowFromMatrix(matrix, 2);
        expectedMatrix = matrix.getSubMatrix(0, 1, 0, 3);
        assertEquals(expectedMatrix, resultMatrix);

        // test remove middle row
        resultMatrix = MathUtil.removeRowFromMatrix(matrix, 1);
        expectedMatrix = matrix.getSubMatrix(new int[] {0, 2}, new int[] {0, 1, 2, 3});
        assertEquals(expectedMatrix, resultMatrix);
    }

    @Test
    public void testAddRowToMatrix() {
        RealMatrix matrix = createFilledMatrix(2, 2);
        RealMatrix resultMatrix = MathUtil.addRowToMatrix(matrix);

        // check matrices are equal when the added row is removed again
        assertEquals(matrix, MathUtil.removeRowFromMatrix(resultMatrix, 2));
        // check appended row is filled with 0
        assertArrayEquals(new double[] {0, 0}, resultMatrix.getRow(2));
    }

    @Test
    public void testRemoveElementFromVector() {
        RealVector vector = createFilledVector(3);

        // test remove first element
        RealVector resultVector = MathUtil.removeElementFromVector(vector, 0);
        double[] expectedVector = new double[] {1, 2};
        assertArrayEquals(expectedVector, resultVector.toArray());

        // test remove last element
        resultVector = MathUtil.removeElementFromVector(vector, 2);
        expectedVector = new double[] {0, 1};
        assertArrayEquals(expectedVector, resultVector.toArray());

        // test remove middle element
        resultVector = MathUtil.removeElementFromVector(vector, 1);
        expectedVector = new double[] {0, 2};
        assertArrayEquals(expectedVector, resultVector.toArray());
    }

    @Test
    public void testAddElementToVector() {
        RealVector vector = createFilledVector(2);
        RealVector resultVector = MathUtil.addElementToVector(vector);

        // check vectors are equal when the added element is removed again
        assertEquals(vector, MathUtil.removeElementFromVector(resultVector, 2));
        // check appended element is 0
        assertEquals(0, resultVector.getEntry(2));

    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
