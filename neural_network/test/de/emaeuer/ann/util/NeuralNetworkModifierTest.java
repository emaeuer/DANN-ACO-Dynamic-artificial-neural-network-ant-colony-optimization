package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;
import de.emaeuer.ann.Neuron;
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
        NeuralNetworkBuilder builder = NeuralNetwork.build()
                .inputLayer(numberOfNeurons[0])
                .fullyConnectToNextLayer();

        for (int i = 1; i < numberOfNeurons.length - 1; i++) {
            builder = builder.hiddenLayer(numberOfNeurons[i])
                    .fullyConnectToNextLayer();
        }

        NeuralNetwork nn = builder.outputLayer(numberOfNeurons[numberOfNeurons.length - 1])
                .finish();

        // set all weights to -1 (for comparison with new weights)
        for (NeuralNetworkLayer layer : nn) {
            if (!layer.isInputLayer()) {
                ((NeuralNetworkLayerImpl) layer).setWeights(layer.getWeights().scalarAdd(-1));
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
        Neuron input = nn.getNeuron(new Neuron.NeuronID(0, 0));
        Neuron output = nn.getNeuron(new Neuron.NeuronID(1, 0));

        input.getConnectionTo(output).splitConnection();

        Neuron newNeuron = nn.getNeuron(new Neuron.NeuronID(1, 0));
        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(1, input.getOutgoingConnections().size());
        assertEquals(1, newNeuron.getIncomingConnections().size());
        assertEquals(1, newNeuron.getOutgoingConnections().size());
        assertEquals(1, output.getIncomingConnections().size());

        // check connection start and end
        assertTrue(input.hasConnectionTo(newNeuron));
        assertTrue(newNeuron.hasConnectionTo(output));
        assertFalse(input.hasConnectionTo(output));

        // check connection configuration
        assertEquals(-1, input.getConnectionTo(newNeuron).getWeight());
        assertEquals(1, newNeuron.getConnectionTo(output).getWeight());

        // check created neuron
        assertEquals(0, newNeuron.getBias());
    }

    @Test
    public void testSplitConnectionRecurrentConsecutiveLayers() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);
        Neuron hidden = nn.getNeuron(new Neuron.NeuronID(1, 0));
        Neuron output = nn.getNeuron(new Neuron.NeuronID(2, 0));

        // add recurrent connection and split it
        nn.modify().addConnection(output.getNeuronID(), hidden.getNeuronID(), 0.5);
        output.getConnectionTo(hidden).splitConnection();

        Neuron newNeuron = nn.getNeuron(new Neuron.NeuronID(2, 0));
        // check general updates
        assertEquals(4, nn.getDepth());
        assertEquals(1, hidden.getOutgoingConnections().size());
        assertEquals(1, newNeuron.getIncomingConnections().size());
        assertEquals(1, newNeuron.getOutgoingConnections().size());
        assertEquals(1, output.getIncomingConnections().size());
        assertEquals(1, output.getOutgoingConnections().size());

        // check connection start and end
        assertTrue(hidden.hasConnectionTo(output));
        assertTrue(newNeuron.hasConnectionTo(hidden));
        assertTrue(output.hasConnectionTo(newNeuron));
        assertFalse(output.hasConnectionTo(hidden));

        // check connection configuration
        assertEquals(0.5, output.getConnectionTo(newNeuron).getWeight());
        assertEquals(1, newNeuron.getConnectionTo(hidden).getWeight());

        // check created neuron
        assertEquals(0, newNeuron.getBias());
    }

    @Test
    public void testSplitConnectionLateral() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        Neuron hiddenOne = nn.getNeuron(new Neuron.NeuronID(1, 0));
        Neuron hiddenTwo = nn.getNeuron(new Neuron.NeuronID(1, 1));

        // add lateral connection and split it
        nn.modify().addConnection(hiddenOne.getNeuronID(), hiddenTwo.getNeuronID(), 0.5);
        hiddenOne.getConnectionTo(hiddenTwo).splitConnection();

        Neuron newNeuron = nn.getNeuron(new Neuron.NeuronID(1, 2));
        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(2, hiddenOne.getOutgoingConnections().size());
        assertEquals(1, hiddenOne.getIncomingConnections().size());
        assertEquals(1, hiddenTwo.getOutgoingConnections().size());
        assertEquals(2, hiddenTwo.getIncomingConnections().size());

        // check connection start and end
        assertTrue(hiddenOne.hasConnectionTo(newNeuron));
        assertTrue(newNeuron.hasConnectionTo(hiddenTwo));
        assertFalse(hiddenOne.hasConnectionTo(hiddenTwo));

        // check connection configuration
        assertEquals(0.5, hiddenOne.getConnectionTo(newNeuron).getWeight());
        assertEquals(1, newNeuron.getConnectionTo(hiddenTwo).getWeight());

        // check created neuron
        assertEquals(0, newNeuron.getBias());
    }

    @Test
    public void testSplitConnectionSelfRecurrent() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);
        Neuron hiddenOne = nn.getNeuron(new Neuron.NeuronID(1, 0));

        // add self recurrent connection and split it
        nn.modify().addConnection(hiddenOne.getNeuronID(), hiddenOne.getNeuronID(), 0.5);
        hiddenOne.getConnectionTo(hiddenOne).splitConnection();

        Neuron newNeuron = nn.getNeuron(new Neuron.NeuronID(1, 1));
        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(2, hiddenOne.getOutgoingConnections().size());
        assertEquals(2, hiddenOne.getIncomingConnections().size());

        // check connection start and end
        assertTrue(hiddenOne.hasConnectionTo(newNeuron));
        assertTrue(newNeuron.hasConnectionTo(hiddenOne));
        assertFalse(hiddenOne.hasConnectionTo(hiddenOne));

        // check connection configuration
        assertEquals(0.5, hiddenOne.getConnectionTo(newNeuron).getWeight());
        assertEquals(1, newNeuron.getConnectionTo(hiddenOne).getWeight());

        // check created neuron
        assertEquals(0, newNeuron.getBias());
    }

    @Test
    public void testSplitConnectionForwardMultipleLayers() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);
        Neuron input = nn.getNeuron(new Neuron.NeuronID(0, 0));
        Neuron hidden = nn.getNeuron(new Neuron.NeuronID(1, 0));
        Neuron output = nn.getNeuron(new Neuron.NeuronID(2, 0));

        // add skip connection and split it
        nn.modify().addConnection(input.getNeuronID(), output.getNeuronID(), 0.5);
        input.getConnectionTo(output).splitConnection();

        Neuron newNeuron = nn.getNeuron(new Neuron.NeuronID(1, 1));
        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(2, input.getOutgoingConnections().size());
        assertEquals(1, newNeuron.getIncomingConnections().size());
        assertEquals(1, newNeuron.getOutgoingConnections().size());
        assertEquals(1, hidden.getIncomingConnections().size());
        assertEquals(1, hidden.getOutgoingConnections().size());
        assertEquals(2, output.getIncomingConnections().size());

        // check connection start and end
        assertTrue(input.hasConnectionTo(newNeuron));
        assertTrue(input.hasConnectionTo(hidden));
        assertTrue(newNeuron.hasConnectionTo(output));
        assertTrue(hidden.hasConnectionTo(output));
        assertFalse(input.hasConnectionTo(output));
        assertFalse(hidden.hasConnectionTo(newNeuron));
        assertFalse(newNeuron.hasConnectionTo(hidden));

        // check connection configuration
        assertEquals(0.5, input.getConnectionTo(newNeuron).getWeight());
        assertEquals(1, newNeuron.getConnectionTo(output).getWeight());

        // check created neuron
        assertEquals(0, newNeuron.getBias());
    }

    @Test
    public void testSplitConnectionRecurrentMultipleLayers() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1, 1);
        Neuron hiddenOne = nn.getNeuron(new Neuron.NeuronID(1, 0));
        Neuron hiddenTwo = nn.getNeuron(new Neuron.NeuronID(2, 0));
        Neuron output = nn.getNeuron(new Neuron.NeuronID(3, 0));

        // add recurrent skip connection and split it
        nn.modify().addConnection(output.getNeuronID(), hiddenOne.getNeuronID(), 0.5);
        output.getConnectionTo(hiddenOne).splitConnection();

        Neuron newNeuron = nn.getNeuron(new Neuron.NeuronID(2, 1));
        // check general updates
        assertEquals(4, nn.getDepth());
        assertEquals(1, hiddenOne.getOutgoingConnections().size());
        assertEquals(2, hiddenOne.getIncomingConnections().size());
        assertEquals(1, newNeuron.getIncomingConnections().size());
        assertEquals(1, newNeuron.getOutgoingConnections().size());
        assertEquals(1, hiddenTwo.getIncomingConnections().size());
        assertEquals(1, hiddenTwo.getOutgoingConnections().size());
        assertEquals(1, output.getIncomingConnections().size());
        assertEquals(1, output.getOutgoingConnections().size());

        // check connection start and end
        assertTrue(hiddenOne.hasConnectionTo(hiddenTwo));
        assertTrue(hiddenTwo.hasConnectionTo(output));
        assertTrue(output.hasConnectionTo(newNeuron));
        assertTrue(newNeuron.hasConnectionTo(hiddenOne));
        assertFalse(output.hasConnectionTo(hiddenOne));

        // check connection configuration
        assertEquals(0.5, output.getConnectionTo(newNeuron).getWeight());
        assertEquals(1, newNeuron.getConnectionTo(hiddenOne).getWeight());

        // check created neuron
        assertEquals(0, newNeuron.getBias());
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
