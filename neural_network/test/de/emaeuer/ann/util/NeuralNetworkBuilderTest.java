package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.Connection;
import de.emaeuer.ann.Neuron;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NeuralNetworkBuilderTest {

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
    public void testNecessaryFieldsHaveToBeSet() {
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build().hiddenLayer(5), "Can't add hidden layer before the input layer");
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build().outputLayer(5), "Can't add output layer before the input layer");
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build().inputLayer(5)
                .fullyConnectToNextLayer()
                .inputLayer(5), "Input layer was already set and can't be overridden");
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build().inputLayer(5)
                .fullyConnectToNextLayer()
                .outputLayer(5)
                .fullyConnectToNextLayer()
                .outputLayer(5), "Output layer was already set and can't be overridden");
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build().inputLayer(5)
                .fullyConnectToNextLayer()
                .outputLayer(5)
                .fullyConnectToNextLayer()
                .inputLayer(5), "Input layer was already set and can't be overridden");
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build().inputLayer(5)
                .fullyConnectToNextLayer()
                .outputLayer(5)
                .fullyConnectToNextLayer()
                .hiddenLayer(5), "Can't add hidden layer after the output layer");
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build()
                .inputLayer(5)
                .finish(), "Failed to create neural network the output layer is missing");
        assertThrows(IllegalStateException.class, () -> NeuralNetwork.build()
                .finish(), "Failed to create neural network the input layer is missing");
    }

    @Test
    public void testBasicCreation() {
        NeuralNetwork nn = NeuralNetwork.build()
                .inputLayer(2)
                .fullyConnectToNextLayer()
                .outputLayer(2)
                .finish();

        // check input layer
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(0, 0)), Arrays.asList(new Neuron.NeuronID(1, 0), new Neuron.NeuronID(1, 1)), new ArrayList<>());
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(0, 1)), Arrays.asList(new Neuron.NeuronID(1, 0), new Neuron.NeuronID(1, 1)), new ArrayList<>());
        // check output layer
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(1, 0)), new ArrayList<>(), Arrays.asList(new Neuron.NeuronID(0, 0), new Neuron.NeuronID(0, 1)));
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(1, 1)), new ArrayList<>(), Arrays.asList(new Neuron.NeuronID(0, 0), new Neuron.NeuronID(0, 1)));
    }

    @Test
    public void testComplexCreation() {
        NeuralNetwork nn = NeuralNetwork.build()
                .inputLayer(3)
                .fullyConnectToNextLayer()
                .hiddenLayer(2)
                .fullyConnectToNextLayer()
                .outputLayer(1)
                .finish();

        // check input layer
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(0, 0)), Arrays.asList(new Neuron.NeuronID(1, 0), new Neuron.NeuronID(1, 1)), new ArrayList<>());
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(0, 1)), Arrays.asList(new Neuron.NeuronID(1, 0), new Neuron.NeuronID(1, 1)), new ArrayList<>());
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(0, 2)), Arrays.asList(new Neuron.NeuronID(1, 0), new Neuron.NeuronID(1, 1)), new ArrayList<>());
        // check input layer
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(1, 0)), Collections.singletonList(new Neuron.NeuronID(2, 0)), Arrays.asList(new Neuron.NeuronID(0, 0), new Neuron.NeuronID(0, 1), new Neuron.NeuronID(0, 2)));
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(1, 1)), Collections.singletonList(new Neuron.NeuronID(2, 0)), Arrays.asList(new Neuron.NeuronID(0, 0), new Neuron.NeuronID(0, 1), new Neuron.NeuronID(0, 2)));
        // check output layer
        checkNeuron(nn.getNeuron(new Neuron.NeuronID(2, 0)), new ArrayList<>(), Arrays.asList(new Neuron.NeuronID(1, 0), new Neuron.NeuronID(1, 1)));
    }


    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    public void checkNeuron(Neuron neuron, List<Neuron.NeuronID> connectTo, List<Neuron.NeuronID> connectFrom) {
        assertEquals(connectTo.size(), neuron.getOutgoingConnections().size());
        assertEquals(connectFrom.size(), neuron.getIncomingConnections().size());

        List<Neuron.NeuronID> connectFromCopy = new ArrayList<>(connectFrom);
        for (Connection incoming : neuron.getIncomingConnections()) {
            assertTrue(connectFromCopy.remove(incoming.start().getNeuronID()));
        }
        assertTrue(connectFromCopy.isEmpty());

        List<Neuron.NeuronID> connectToCopy = new ArrayList<>(connectTo);
        for (Connection outgoing : neuron.getOutgoingConnections()) {
            assertTrue(connectToCopy.remove(outgoing.end().getNeuronID()));
        }
        assertTrue(connectToCopy.isEmpty());
    }

}
