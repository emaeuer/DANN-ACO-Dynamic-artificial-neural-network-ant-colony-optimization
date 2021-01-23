//package de.uni.test.ann2;
//
//import de.uni.ann2.Connection;
//import de.uni.ann2.NeuralNetwork;
//import de.uni.ann2.NeuralNetworkLayer;
//import de.uni.ann2.Neuron;
//import org.apache.commons.math3.linear.ArrayRealVector;
//import org.apache.commons.math3.linear.MatrixUtils;
//import org.apache.commons.math3.linear.RealMatrix;
//import org.apache.commons.math3.linear.RealVector;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class NeuralNetworkLayerBuilderTest {
//
//    /*
//     ##########################################################
//     ################# Data creation Methods ##################
//     ##########################################################
//    */
//
//    private static final String EXCEPTION_MESSAGE_PATTERN = "Failed to create neural network layer because attribute \"%s\" was not set";
//
//
//    /*
//     ##########################################################
//     ##################### Test Methods #######################
//     ##########################################################
//    */
//
//    @Test
//    public void testNecessaryFieldsHaveToBeSet() {
////        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayer.build().finish(),
////                String.format(EXCEPTION_MESSAGE_PATTERN, "NumberOfNeurons"));
////        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayer.build().numberOfNeurons(1).finish(),
////                String.format(EXCEPTION_MESSAGE_PATTERN, "LayerType"));
////        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayer.build().numberOfNeurons(1).layerType(NeuralNetworkLayer.LayerType.INPUT).finish(),
////                String.format(EXCEPTION_MESSAGE_PATTERN, "NeuralNetwork"));
////        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayer.build().numberOfNeurons(1).layerType(NeuralNetworkLayer.LayerType.INPUT).neuralNetwork(new NeuralNetwork()).finish(),
////                String.format(EXCEPTION_MESSAGE_PATTERN, "LayerID"));
////        assertDoesNotThrow(() -> NeuralNetworkLayer.build().numberOfNeurons(1).layerType(NeuralNetworkLayer.LayerType.INPUT).neuralNetwork(new NeuralNetwork()).layerID(0).finish());
//    }
//
//    @Test
//    public void successfulCreationOfInputLayer() {
////        NeuralNetworkLayer layer = NeuralNetworkLayer.build()
////                .numberOfNeurons(5)
////                .layerType(NeuralNetworkLayer.LayerType.INPUT)
////                .neuralNetwork(new NeuralNetwork())
////                .layerID(0)
////                .finish();
////
////        // test properties
////        assertTrue(layer.isInputLayer());
////        assertFalse(layer.isOutputLayer());
////        assertEquals(5, layer.getNumberOfNeurons());
////        assertEquals(0, layer.getLayerID());
////        assertEquals(new ArrayRealVector(5), layer.getActivation());
////        assertNull(layer.getBias());
////        assertNull(layer.getWeights());
////
////        // test processing
////        RealVector input = new ArrayRealVector(new double[] {1, 2, 3, 4, 5});
////        RealVector output = layer.process(input);
////        assertEquals(input, output);
////        assertEquals(input, layer.getActivation());
//    }
//
//    @Test
//    public void successfulCreationOfHiddenLayer() {
////        NeuralNetwork nn = new NeuralNetwork();
////
////        NeuralNetworkLayer inputLayer = NeuralNetworkLayer.build()
////                .numberOfNeurons(2)
////                .layerType(NeuralNetworkLayer.LayerType.INPUT)
////                .neuralNetwork(nn)
////                .layerID(0)
////                .finish();
////
////        RealVector bias = new ArrayRealVector(new double[] {0.5, -0.5});
////        RealMatrix expectedWeights = MatrixUtils.createRealMatrix(new double[][]{{2, 0}, {0, 0.5}});
////
////        NeuralNetworkLayer layer = NeuralNetworkLayer.build()
////                .numberOfNeurons(2)
////                .layerType(NeuralNetworkLayer.LayerType.HIDDEN)
////                .neuralNetwork(nn)
////                .layerID(1)
////                .addConnection(new Connection.ConnectionPrototype(new Neuron.NeuronID(0, 0), new Neuron.NeuronID(1, 0), 2),
////                        new Connection.ConnectionPrototype(new Neuron.NeuronID(0, 1), new Neuron.NeuronID(1, 1), 0.5))
////                .bias(bias)
////                .finish();
////
////        // test properties
////        assertFalse(layer.isInputLayer());
////        assertFalse(layer.isOutputLayer());
////        assertEquals(2, layer.getNumberOfNeurons());
////        assertEquals(1, layer.getLayerID());
////        assertEquals(new ArrayRealVector(2), layer.getActivation());
////        assertEquals(bias, layer.getBias());
////        assertEquals(expectedWeights, layer.getWeights());
////
////        // test processing
////        RealVector input = new ArrayRealVector(new double[] {1, 2});
////        RealVector expectedOutput = new ArrayRealVector(new double[] {2.5, 0.5});
////        RealVector output = layer.process(input);
////        assertEquals(expectedOutput, output);
////        assertEquals(expectedOutput, layer.getActivation());
//    }
//
//
//    /*
//     ##########################################################
//     #################### Helper Methods ######################
//     ##########################################################
//    */
//
//}
