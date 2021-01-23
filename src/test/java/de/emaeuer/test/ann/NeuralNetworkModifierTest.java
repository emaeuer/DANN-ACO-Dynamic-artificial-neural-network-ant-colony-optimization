package de.emaeuer.test.ann;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkModifier;
import de.emaeuer.ann.Neuron;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkModifierTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */


    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testNavigationInNetwork() {
        NeuralNetwork nn = new NeuralNetwork(2, 2, 2, 2);
        NeuralNetworkModifier modifier = nn.getModifier();

        assertThrows(IndexOutOfBoundsException.class, () -> modifier.modifyLayer(0));
        assertThrows(IndexOutOfBoundsException.class, () -> modifier.modifyLayer(4));

        assertDoesNotThrow(() -> modifier.restartModification()
                .finish()
                .modifyNextLayer()
                .finish()
                .modifyNextLayer()
                .finish()
                .modifyPreviousLayer()
                .finish()
                .modifyPreviousLayer());

        assertThrows(IndexOutOfBoundsException.class, () -> modifier.restartModification()
                .finish()
                .modifyPreviousLayer());

        assertThrows(IndexOutOfBoundsException.class, () -> modifier.modifyLayer(3)
                .finish()
                .modifyNextLayer());
    }

    @Test
    public void testWeightModification() {
        NeuralNetwork nn = new NeuralNetwork(2, 2, 2, 2);
        NeuralNetworkModifier modifier = nn.getModifier();

        // test modification of single neuron
        modifier.modifyLayer(1).setWeightOfConnection(1, 0, 4);
        assertEquals(4, nn.getNeuron(new Neuron.NeuronID(0, 1)).getConnections().get(0).getWeight());

        // test modification of weight vector of a neuron
        modifier.modifyLayer(2).setWeightsOfNeuron(1, new ArrayRealVector(new double[]{5, 6}));
        assertEquals(5, nn.getNeuron(new Neuron.NeuronID(1, 1)).getConnections().get(0).getWeight());
        assertEquals(6, nn.getNeuron(new Neuron.NeuronID(1, 1)).getConnections().get(1).getWeight());

        // test modification of weight matrix
        modifier.modifyLayer(3).setWeightsOfLayer(MatrixUtils.createRealMatrix(new double[][]{{7, 8}, {9, 10}}));
        assertEquals(7, nn.getNeuron(new Neuron.NeuronID(2, 0)).getConnections().get(0).getWeight());
        assertEquals(9, nn.getNeuron(new Neuron.NeuronID(2, 0)).getConnections().get(1).getWeight());
        assertEquals(8, nn.getNeuron(new Neuron.NeuronID(2, 1)).getConnections().get(0).getWeight());
        assertEquals(10, nn.getNeuron(new Neuron.NeuronID(2, 1)).getConnections().get(1).getWeight());
    }

    @Test
    public void testBiasModification() {
        NeuralNetwork nn = new NeuralNetwork(2, 2, 2, 2);
        NeuralNetworkModifier modifier = nn.getModifier();

        // test modification of single bias
        modifier.modifyLayer(2).setBiasOfNeuron(1, 5);
        assertEquals(5, nn.getNeuron(new Neuron.NeuronID(2, 1)).getBias());

        // test modification of bias vector
        modifier.modifyLayer(3).setBiasOfLayer(new ArrayRealVector(new double[]{4,6}));
        assertEquals(4, nn.getNeuron(new Neuron.NeuronID(3, 0)).getBias());
        assertEquals(6, nn.getNeuron(new Neuron.NeuronID(3, 1)).getBias());
    }


    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */


}
