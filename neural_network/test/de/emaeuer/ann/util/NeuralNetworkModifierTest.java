package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.impl.NeuralNetworkBuilderImpl;
import de.emaeuer.ann.impl.NeuralNetworkImpl;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkModifierTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private NeuralNetwork buildNeuralNetwork(int... numberOfNeurons) {
        if (numberOfNeurons.length < 2) {
            fail("A neural network needs at least 2 layers");
        }
        NeuralNetworkBuilderImpl builder = (NeuralNetworkBuilderImpl) NeuralNetwork.build()
                .inputLayer(numberOfNeurons[0])
                .fullyConnectToNextLayer();

        for (int i = 1; i < numberOfNeurons.length - 1; i++) {
            builder = builder.hiddenLayer(numberOfNeurons[i])
                    .fullyConnectToNextLayer();
        }

        NeuralNetworkImpl nn = (NeuralNetworkImpl) builder.outputLayer(numberOfNeurons[numberOfNeurons.length - 1])
                .finish();

        // set all weights to -1 (for comparison with new weights)
        for (NeuralNetworkLayerImpl layer : nn.getLayers()) {
            if (!layer.isInputLayer()) {
                layer.setWeights(layer.getWeights().scalarAdd(-1));
            }
        }

        return nn;
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testSplitConnectionForwardConsecutiveLayers() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1);
        NeuronID input = new NeuronID(0, 0);

        nn.modify().splitConnection(new NeuronID(0, 0), new NeuronID(1, 0));

        NeuronID newNeuron = new NeuronID(1, 0);
        NeuronID output = new NeuronID(2, 0);

        assertEquals(newNeuron, nn.modify().getLastModifiedNeuron());

        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(input).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(output).size());

        // check connection start and end
        assertTrue(nn.neuronHasConnectionTo(input, newNeuron));
        assertTrue(nn.neuronHasConnectionTo(newNeuron, output));
        assertFalse(nn.neuronHasConnectionTo(input, output));

        // check connection configuration
        assertEquals(-1, nn.getWeightOfConnection(input, newNeuron));
        assertEquals(1, nn.getWeightOfConnection(newNeuron, output));

        // check created neuron
        assertEquals(0, nn.getBiasOfNeuron(newNeuron));
    }

    @Test
    public void testSplitConnectionRecurrentConsecutiveLayers() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);
        NeuronID hidden = new NeuronID(1, 0);
        NeuronID output = new NeuronID(2, 0);

        // add recurrent connection and split it
        nn.modify()
                .addConnection(output, hidden, 0.5)
                .splitConnection(output, hidden);

        NeuronID newNeuron = new NeuronID(2, 0);
        output = new NeuronID(3, 0);

        assertEquals(newNeuron, nn.modify().getLastModifiedNeuron());

        // check general updates
        assertEquals(4, nn.getDepth());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(hidden).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(output).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(output).size());

        // check connection start and end
        assertTrue(nn.neuronHasConnectionTo(hidden, output));
        assertTrue(nn.neuronHasConnectionTo(newNeuron, hidden));
        assertTrue(nn.neuronHasConnectionTo(output, newNeuron));
        assertFalse(nn.neuronHasConnectionTo(output, hidden));

        // check connection configuration
        assertEquals(0.5, nn.getWeightOfConnection(output, newNeuron));
        assertEquals(1, nn.getWeightOfConnection(newNeuron, hidden));

        // check created neuron
        assertEquals(0, nn.getBiasOfNeuron(newNeuron));
    }

    @Test
    public void testSplitConnectionLateral() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuronID hiddenOne = new NeuronID(1, 0);
        NeuronID hiddenTwo = new NeuronID(1, 1);

        // add lateral connection and split it
        nn.modify()
                .addConnection(hiddenOne, hiddenTwo, 0.5)
                .splitConnection(hiddenOne, hiddenTwo);

        NeuronID newNeuron = new NeuronID(1, 2);

        assertEquals(newNeuron, nn.modify().getLastModifiedNeuron());

        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(2, nn.getOutgoingConnectionsOfNeuron(hiddenOne).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(hiddenOne).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(hiddenTwo).size());
        assertEquals(2, nn.getIncomingConnectionsOfNeuron(hiddenTwo).size());

        // check connection start and end
        assertTrue(nn.neuronHasConnectionTo(hiddenOne, newNeuron));
        assertTrue(nn.neuronHasConnectionTo(newNeuron, hiddenTwo));
        assertFalse(nn.neuronHasConnectionTo(hiddenOne, hiddenTwo));

        // check connection configuration
        assertEquals(0.5, nn.getWeightOfConnection(hiddenOne, newNeuron));
        assertEquals(1, nn.getWeightOfConnection(newNeuron, hiddenTwo));

        // check created neuron
        assertEquals(0, nn.getBiasOfNeuron(newNeuron));
    }

    @Test
    public void testSplitConnectionSelfRecurrent() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);
        NeuronID hiddenOne = new NeuronID(1, 0);

        // add self recurrent connection and split it
        nn.modify()
                .addConnection(hiddenOne, hiddenOne, 0.5)
                .splitConnection(hiddenOne, hiddenOne);

        NeuronID newNeuron = new NeuronID(1, 1);

        assertEquals(newNeuron, nn.modify().getLastModifiedNeuron());

        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(2, nn.getOutgoingConnectionsOfNeuron(hiddenOne).size());
        assertEquals(2, nn.getIncomingConnectionsOfNeuron(hiddenOne).size());

        // check connection start and end
        assertTrue(nn.neuronHasConnectionTo(hiddenOne, newNeuron));
        assertTrue(nn.neuronHasConnectionTo(newNeuron, hiddenOne));
        assertFalse(nn.neuronHasConnectionTo(hiddenOne, hiddenOne));

        // check connection configuration
        assertEquals(0.5, nn.getWeightOfConnection(hiddenOne, newNeuron));
        assertEquals(1, nn.getWeightOfConnection(newNeuron, hiddenOne));

        // check created neuron
        assertEquals(0, nn.getBiasOfNeuron(newNeuron));
    }

    @Test
    public void testSplitConnectionForwardMultipleLayers() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);
        NeuronID input = new NeuronID(0, 0);
        NeuronID hidden = new NeuronID(1, 0);
        NeuronID output = new NeuronID(2, 0);

        // add skip connection and split it
        nn.modify().addConnection(input, output, 0.5)
                .splitConnection(input, output);

        NeuronID newNeuron = new NeuronID(1, 1);

        assertEquals(newNeuron, nn.modify().getLastModifiedNeuron());

        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(2, nn.getOutgoingConnectionsOfNeuron(input).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(hidden).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(hidden).size());
        assertEquals(2, nn.getIncomingConnectionsOfNeuron(output).size());

        // check connection start and end
        assertTrue(nn.neuronHasConnectionTo(input, newNeuron));
        assertTrue(nn.neuronHasConnectionTo(input, hidden));
        assertTrue(nn.neuronHasConnectionTo(newNeuron, output));
        assertTrue(nn.neuronHasConnectionTo(hidden, output));
        assertFalse(nn.neuronHasConnectionTo(input, output));
        assertFalse(nn.neuronHasConnectionTo(hidden, newNeuron));
        assertFalse(nn.neuronHasConnectionTo(newNeuron, hidden));

        // check connection configuration
        assertEquals(0.5, nn.getWeightOfConnection(input, newNeuron));
        assertEquals(1, nn.getWeightOfConnection(newNeuron, output));

        // check created neuron
        assertEquals(0, nn.getBiasOfNeuron(newNeuron));
    }

    @Test
    public void testSplitConnectionRecurrentMultipleLayers() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1, 1);
        NeuronID hiddenOne = new NeuronID(1, 0);
        NeuronID hiddenTwo = new NeuronID(2, 0);
        NeuronID output = new NeuronID(3, 0);

        // add recurrent skip connection and split it
        nn.modify()
                .addConnection(output, hiddenOne, 0.5)
                .splitConnection(output, hiddenOne);

        NeuronID newNeuron = new NeuronID(2, 1);

        assertEquals(newNeuron, nn.modify().getLastModifiedNeuron());

        // check general updates
        assertEquals(4, nn.getDepth());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(hiddenOne).size());
        assertEquals(2, nn.getIncomingConnectionsOfNeuron(hiddenOne).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(newNeuron).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(hiddenTwo).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(hiddenTwo).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(output).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(output).size());

        // check connection start and end
        assertTrue(nn.neuronHasConnectionTo(hiddenOne, hiddenTwo));
        assertTrue(nn.neuronHasConnectionTo(hiddenTwo, output));
        assertTrue(nn.neuronHasConnectionTo(output, newNeuron));
        assertTrue(nn.neuronHasConnectionTo(newNeuron, hiddenOne));
        assertFalse(nn.neuronHasConnectionTo(output, hiddenOne));

        // check connection configuration
        assertEquals(0.5, nn.getWeightOfConnection(output, newNeuron));
        assertEquals(1, nn.getWeightOfConnection(newNeuron, hiddenOne));

        // check created neuron
        assertEquals(0, nn.getBiasOfNeuron(newNeuron));
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
