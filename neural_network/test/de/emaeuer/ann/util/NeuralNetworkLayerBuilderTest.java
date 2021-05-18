package de.emaeuer.ann.util;

import de.emaeuer.ann.LayerType;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkLayerImpl;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkLayerBuilderTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private static final String EXCEPTION_MESSAGE_PATTERN = "Failed to create neural network layer because attribute \"%s\" was not set";


    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testNecessaryFieldsHaveToBeSet() {
        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayerImpl.build().finish(),
                String.format(EXCEPTION_MESSAGE_PATTERN, "NumberOfNeurons"));
        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayerImpl.build().numberOfNeurons(1).finish(),
                String.format(EXCEPTION_MESSAGE_PATTERN, "LayerType"));
        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayerImpl.build().numberOfNeurons(1).layerType(LayerType.INPUT).finish(),
                String.format(EXCEPTION_MESSAGE_PATTERN, "NeuralNetwork"));
        assertThrows(IllegalStateException.class, () -> NeuralNetworkLayerImpl.build().numberOfNeurons(1).layerType(LayerType.INPUT).neuralNetwork(new NeuralNetworkImpl()).finish(),
                String.format(EXCEPTION_MESSAGE_PATTERN, "LayerID"));
        assertDoesNotThrow(() -> NeuralNetworkLayerImpl.build().numberOfNeurons(1).layerType(LayerType.INPUT).neuralNetwork(new NeuralNetworkImpl()).layerID(0).finish());
    }

    @Test
    public void successfulCreationOfInputLayer() {
        NeuralNetworkLayerImpl layer = NeuralNetworkLayerImpl.build()
                .numberOfNeurons(5)
                .layerType(LayerType.INPUT)
                .neuralNetwork(new NeuralNetworkImpl())
                .layerID(0)
                .finish();

        // test properties
        assertTrue(layer.isInputLayer());
        assertFalse(layer.isOutputLayer());
        assertEquals(5, layer.getNumberOfNeurons());
        assertEquals(0, layer.getLayerIndex());
        assertEquals(new ArrayRealVector(5), layer.getActivation());
        assertNull(layer.getBias());
        assertNull(layer.getWeights());

        // test processing
        RealVector input = new ArrayRealVector(new double[] {1, 2, 3, 4, 5});
        RealVector output = layer.process(input);
        assertEquals(input, output);
        assertEquals(input, layer.getActivation());
    }

    @Test
    public void successfulCreationOfHiddenLayer() {
        RealVector bias = new ArrayRealVector(new double[] {0.5, -0.5});
        RealMatrix expectedWeights = MatrixUtils.createRealMatrix(new double[][]{{2, 0}, {0, 0.5}});

        NeuralNetworkImpl nn = (NeuralNetworkImpl) NeuralNetwork.build()
                .inputLayer(2)
                .hiddenLayer(b -> b.numberOfNeurons(2)
                        .addConnection(new NeuronID(0, 0), new NeuronID(1, 0), 2)
                        .addConnection(new NeuronID(0, 1), new NeuronID(1, 1), 0.5)
                        .bias(bias))
                .fullyConnectToNextLayer()
                .outputLayer(2)
                .finish();


        NeuralNetworkLayerImpl layer = nn.getLayer(1);

        // test properties
        assertFalse(layer.isInputLayer());
        assertFalse(layer.isOutputLayer());
        assertEquals(2, layer.getNumberOfNeurons());
        assertEquals(1, layer.getLayerIndex());
        assertEquals(new ArrayRealVector(2), layer.getActivation());
        assertEquals(bias, layer.getBias());
        assertEquals(expectedWeights, layer.getWeights());

        // test processing
        RealVector input = new ArrayRealVector(new double[] {1, 2});
        RealVector expectedOutput = new ArrayRealVector(new double[] {2.5, 0.5});
        // pass input to input layer (no processing) and afterwards let the hidden layer process
        nn.process(input);
        RealVector output = layer.process();
        assertEquals(expectedOutput, output);
        assertEquals(expectedOutput, layer.getActivation());
    }

    @Test
    public void successfulCreationOfOutputLayer() {
        RealVector bias = new ArrayRealVector(new double[] {0.5, -0.5});
        RealMatrix expectedWeights = MatrixUtils.createRealMatrix(new double[][]{{2}, {0.5}});

        NeuralNetworkImpl nn = (NeuralNetworkImpl) NeuralNetwork.build()
                .inputLayer(2)
                .fullyConnectToNextLayer()
                .hiddenLayer(2)
                .outputLayer(b -> b.numberOfNeurons(2)
                        .addConnection(new NeuronID(1, 0), new NeuronID(2, 0), 2)
                        .addConnection(new NeuronID(1, 0), new NeuronID(2, 1), 0.5)
                        .bias(bias))
                .finish();


        NeuralNetworkLayerImpl layer = nn.getLayer(2);

        // test properties
        assertFalse(layer.isInputLayer());
        assertTrue(layer.isOutputLayer());
        assertEquals(2, layer.getNumberOfNeurons());
        assertEquals(2, layer.getLayerIndex());
        assertEquals(new ArrayRealVector(2), layer.getActivation());
        assertEquals(bias, layer.getBias());
        assertEquals(expectedWeights, layer.getWeights());
    }


    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
