//package de.uni.test.aco;
//
//import de.uni.optimization.aco.Ant;
//import de.uni.optimization.aco.pheromone.CompositePheromoneMatrix;
//import de.uni.optimization.aco.pheromone.LayerPheromoneMatrix;
//import de.uni.ann.NeuralNetwork;
//import de.uni.ann.Neuron;
//import org.apache.commons.math3.linear.MatrixUtils;
//import org.junit.jupiter.api.Test;
//
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class CompositePheromoneMatrixTest {
//
//    /*
//     ##########################################################
//     ################# Data creation Methods ##################
//     ##########################################################
//    */
//
//    private CompositePheromoneMatrix createSimplePheromoneMatrix() {
//        NeuralNetwork nn = new NeuralNetwork(2, 2, 1);
//        return CompositePheromoneMatrix.buildForNeuralNetwork(nn);
//    }
//
//    /*
//     ##########################################################
//     ##################### Test Methods #######################
//     ##########################################################
//    */
//
//    @Test
//    public void testCreation() {
//        CompositePheromoneMatrix matrix = createSimplePheromoneMatrix();
//
//        assertEquals(2, matrix.size());
//        Iterator<LayerPheromoneMatrix> matrices = matrix.iterator();
//
//        LayerPheromoneMatrix first = matrices.next();
//        checkMatrixDimensions(first, 2, 2);
//        checkMatrixCorrectlyFilled(new double[][]{{0, 0}, {0, 0}}, first);
//
//        LayerPheromoneMatrix second = matrices.next();
//        checkMatrixDimensions(second, 1, 2);
//        checkMatrixCorrectlyFilled(new double[][]{{0, 0}}, second);
//    }
//
//    @Test
//    public void testUpdateSolution() {
//        CompositePheromoneMatrix matrix = createSimplePheromoneMatrix();
//        List<Ant.Decision> solution = Arrays.asList(
//                new Ant.Decision(new Neuron.NeuronID(0, 0), false, false),
//                new Ant.Decision(new Neuron.NeuronID(1, 1), true, false),
//                new Ant.Decision(new Neuron.NeuronID(2, 0), false, false));
//
//        matrix.updateSolution(solution);
//
//        Iterator<LayerPheromoneMatrix> matrices = matrix.iterator();
//
//        LayerPheromoneMatrix first = matrices.next();
//        checkMatrixDimensions(first, 2, 2);
//        checkMatrixCorrectlyFilled(new double[][]{{0, 0}, {0, 0}}, new double[][]{{0, 0}, {0.5, 0}}, first);
//
//        LayerPheromoneMatrix second = matrices.next();
//        checkMatrixDimensions(second, 1, 2);
//        checkMatrixCorrectlyFilled(new double[][]{{0, 0.5}}, new double[][]{{0, 0}}, second);
//    }
//
//    @Test
//    public void testAverageCalculation() {
//        CompositePheromoneMatrix matrix1 = createSimplePheromoneMatrix();
//        CompositePheromoneMatrix matrix2 = createSimplePheromoneMatrix();
//        CompositePheromoneMatrix matrix3 = createSimplePheromoneMatrix();
//
//        List<Ant.Decision> solution = Arrays.asList(
//                new Ant.Decision(new Neuron.NeuronID(0, 0), false, false),
//                new Ant.Decision(new Neuron.NeuronID(1, 1), true, false),
//                new Ant.Decision(new Neuron.NeuronID(2, 0), false, false));
//
//        matrix2.updateSolution(solution);
//        matrix3.updateSolution(solution);
//
//        CompositePheromoneMatrix average = CompositePheromoneMatrix.calculateAverage(Arrays.asList(matrix1, matrix2, matrix3));
//
//        Iterator<LayerPheromoneMatrix> matrices = average.iterator();
//
//        LayerPheromoneMatrix first = matrices.next();
//        checkMatrixDimensions(first, 2, 2);
//        checkMatrixCorrectlyFilled(new double[][]{{0, 0}, {0, 0}}, new double[][]{{0, 0}, {0.5, 0}}, first);
//
//        LayerPheromoneMatrix second = matrices.next();
//        checkMatrixDimensions(second, 1, 2);
//        checkMatrixCorrectlyFilled(new double[][]{{0, 0.5}}, new double[][]{{0, 0}}, second);
//    }
//
//    /*
//     ##########################################################
//     #################### Helper Methods ######################
//     ##########################################################
//    */
//
//    private void checkMatrixDimensions(LayerPheromoneMatrix pheromone, int rowDim, int colDim) {
//        assertEquals(rowDim, pheromone.getWeightExcitatory().getRowDimension());
//        assertEquals(colDim, pheromone.getWeightExcitatory().getColumnDimension());
//        assertEquals(rowDim, pheromone.getWeightInhibitory().getRowDimension());
//        assertEquals(colDim, pheromone.getWeightInhibitory().getColumnDimension());
//    }
//
//    private void checkMatrixCorrectlyFilled(double[][] expectedIncrement, LayerPheromoneMatrix pheromone) {
//        checkMatrixCorrectlyFilled(expectedIncrement, expectedIncrement, pheromone);
//    }
//
//    private void checkMatrixCorrectlyFilled(double[][] expectedIncExc, double[][] expectedIncInh, LayerPheromoneMatrix pheromone) {
//        double pheromoneValue = CompositePheromoneMatrix.INITIAL_PHEROMONE_VALUE;
//
//        // always add initial existing value
//        assertEquals(MatrixUtils.createRealMatrix(expectedIncExc).scalarAdd(pheromoneValue), pheromone.getWeightExcitatory());
//        assertEquals(MatrixUtils.createRealMatrix(expectedIncInh).scalarAdd(pheromoneValue), pheromone.getWeightInhibitory());
//    }
//
//}
