package de.emaeuer.ann;

import de.emaeuer.ann.impl.NeuralNetworkImpl;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class NeuralNetworkTest {

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
    public void testXOR() {
        NeuralNetworkImpl nn = (NeuralNetworkImpl) NeuralNetwork.build()
                .inputLayer(2)
                .hiddenLayer(b -> b.numberOfNeurons(2)
                        .activationFunction(ActivationFunctions.RELU)
                        .addConnection(new NeuronID(0, 0), new NeuronID(1, 0), 1)
                        .addConnection(new NeuronID(0, 0), new NeuronID(1, 1), -1)
                        .addConnection(new NeuronID(0, 1), new NeuronID(1, 0), -1)
                        .addConnection(new NeuronID(0, 1), new NeuronID(1, 1), 1))
                .outputLayer(b -> b.numberOfNeurons(1)
                        .activationFunction(ActivationFunctions.RELU)
                        .addConnection(new NeuronID(1, 0), new NeuronID(2, 0), 1)
                        .addConnection(new NeuronID(1, 1), new NeuronID(2, 0), 1))
                .finish();

        RealVector input = new ArrayRealVector(2);

        // Test 0 ^ 0
        input.setEntry(0, 0);
        input.setEntry(1, 0);
        assertArrayEquals(new double[]{0}, nn.process(input).toArray());
        assertArrayEquals(new double[]{0, 0}, nn.getLayer(1).getActivation().toArray());

        // Test 0 ^ 1
        input.setEntry(0, 0);
        input.setEntry(1, 1);
        assertArrayEquals(new double[]{1}, nn.process(input).toArray());
        assertArrayEquals(new double[]{0, 1}, nn.getLayer(1).getActivation().toArray());
        // Test 1 ^ 0
        input.setEntry(0, 1);
        input.setEntry(1, 0);
        assertArrayEquals(new double[]{1}, nn.process(input).toArray());
        assertArrayEquals(new double[]{1, 0}, nn.getLayer(1).getActivation().toArray());
        // Test 1 ^ 1
        input.setEntry(0, 1);
        input.setEntry(1, 1);
        assertArrayEquals(new double[]{0}, nn.process(input).toArray());
        assertArrayEquals(new double[]{0, 0}, nn.getLayer(1).getActivation().toArray());
    }



    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
