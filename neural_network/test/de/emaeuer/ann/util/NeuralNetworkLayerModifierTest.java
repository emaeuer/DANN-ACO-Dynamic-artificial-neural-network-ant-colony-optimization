package de.emaeuer.ann.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.impl.NeuralNetworkLayerImpl;
import de.emaeuer.ann.Neuron;
import de.emaeuer.ann.Neuron.NeuronID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkLayerModifierTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private NeuralNetworkLayer buildHiddenLayer(int numberOfNeurons) {
        return NeuralNetwork.build()
                .inputLayer(1)
                .fullyConnectToNextLayer()
                .hiddenLayer(numberOfNeurons)
                .fullyConnectToNextLayer()
                .outputLayer(1)
                .finish()
                .getLayer(1);
    }

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
    public void testAddNeuron() {
        NeuralNetworkLayer layer = buildHiddenLayer(1);

        layer.modify().addNeuron(0.5);

        checkNumberOfNeurons(2, layer);

        // check weights of new neuron
        assertArrayEquals(new double[]{0}, layer.getWeights().getRow(1));
        // check bias of neuron
        assertEquals(0.5, layer.getBias().getEntry(1));
        // check activation of neuron
        assertEquals(0, layer.getActivation().getEntry(1));
    }

    @Test
    public void testAddNeuronError() {
        NeuralNetwork nn = buildNeuralNetwork(2, 2);

        UnsupportedOperationException e1 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(0).modify().addNeuron(0));
        assertEquals("Adding neurons to the input or output layer is not supported", e1.getMessage());

        UnsupportedOperationException e2 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(1).modify().addNeuron(0));
        assertEquals("Adding neurons to the input or output layer is not supported", e2.getMessage());
    }

    @Test
    public void testRemoveNeuron() {
        NeuralNetwork nn = buildNeuralNetwork(1, 3, 1);
        NeuralNetworkLayer hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayer outputLayer = nn.getLayer(2);

        hiddenLayer.modify().removeNeuron(0);

        checkNumberOfNeurons(2, hiddenLayer);

        // check remaining neurons were shifted
        assertEquals(0, hiddenLayer.getNeuron(0).getNeuronInLayerID());
        assertEquals(1, hiddenLayer.getNeuron(1).getNeuronInLayerID());
        // check weight matrices
        assertEquals(1, outputLayer.getWeights().getRowDimension());
        assertArrayEquals(new double[]{-1, -1}, outputLayer.getWeights().getRow(0));
        assertEquals(2, hiddenLayer.getWeights().getRowDimension());
        assertArrayEquals(new double[]{-1}, hiddenLayer.getWeights().getRow(0));
        assertArrayEquals(new double[]{-1}, hiddenLayer.getWeights().getRow(1));
        // check bias vector
        assertArrayEquals(new double[]{0, 0}, hiddenLayer.getBias().toArray());
        // check activation vector
        assertArrayEquals(new double[]{0, 0}, hiddenLayer.getActivation().toArray());
        // check neurons and their connections
        checkIsOneTwoOneNetWithAdditionalConnection(nn, null, null);
    }

    @Test
    public void testRemoveNeuronError() {
        NeuralNetwork nn = buildNeuralNetwork(2, 1, 2);

        UnsupportedOperationException e1 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(0).modify().removeNeuron(0));
        assertEquals("Removing neurons from the input or output layer is not supported", e1.getMessage());

        UnsupportedOperationException e2 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(2).modify().removeNeuron(0));
        assertEquals("Removing neurons from the input or output layer is not supported", e2.getMessage());

        IllegalStateException e3 = assertThrows(IllegalStateException.class, () -> nn.getLayer(1).modify().removeNeuron(0));
        assertEquals("Removing the neuron isn't possible because it is the last one in this layer. Delete the layer instead", e3.getMessage());
    }

    @Test
    public void testAddForwardConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer inputLayer = nn.getLayer(0);
        NeuralNetworkLayer hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = (NeuralNetworkLayerImpl) nn.getLayer(2);

        outputLayer.modify().addConnection(inputLayer.getNeuron(0), outputLayer.getNeuron(0), 1);

        // check input neurons
        assertEquals(3, outputLayer.getInputNeurons().size());
        assertTrue(outputLayer.getInputNeurons().contains(inputLayer.getNeuron(0)));
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeuron(0)));
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeuron(1)));

        // check weights
        assertEquals(1, outputLayer.getWeights().getRowDimension());
        assertEquals(3, outputLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, -1, 1}, outputLayer.getWeights().getRow(0));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, new NeuronID(0, 0), new NeuronID(2, 0));
    }

    @Test
    public void testAddLateralConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl layer = (NeuralNetworkLayerImpl) nn.getLayer(1);

        layer.modify().addConnection(layer.getNeuron(0), layer.getNeuron(1), 1);

        // check input neurons
        assertEquals(2, layer.getInputNeurons().size());
        assertTrue(layer.getInputNeurons().contains(inputLayer.getNeuron(0)));
        assertTrue(layer.getInputNeurons().contains(layer.getNeuron(0)));

        // check weights
        assertEquals(2, layer.getWeights().getRowDimension());
        assertEquals(2, layer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, 0}, layer.getWeights().getRow(0));
        assertArrayEquals(new double[]{-1, 1}, layer.getWeights().getRow(1));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, new NeuronID(1, 0), new NeuronID(1, 1));
    }

    @Test
    public void testAddRecurrentConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = (NeuralNetworkLayerImpl) nn.getLayer(1);
        NeuralNetworkLayer outputLayer = nn.getLayer(2);

        hiddenLayer.modify().addConnection(outputLayer.getNeuron(0), hiddenLayer.getNeuron(0), 1);

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeuron(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(outputLayer.getNeuron(0)));

        // check weights
        assertEquals(2, hiddenLayer.getWeights().getRowDimension());
        assertEquals(2, hiddenLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, 1}, hiddenLayer.getWeights().getRow(0));
        assertArrayEquals(new double[]{-1, 0}, hiddenLayer.getWeights().getRow(1));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, new NeuronID(2, 0), new NeuronID(1, 0));
    }

    @Test
    public void testAddSelfRecurrentConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = (NeuralNetworkLayerImpl) nn.getLayer(1);

        hiddenLayer.modify().addConnection(hiddenLayer.getNeuron(0), hiddenLayer.getNeuron(0), 1);

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeuron(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(hiddenLayer.getNeuron(0)));

        // check weights
        assertEquals(2, hiddenLayer.getWeights().getRowDimension());
        assertEquals(2, hiddenLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, 1}, hiddenLayer.getWeights().getRow(0));
        assertArrayEquals(new double[]{-1, 0}, hiddenLayer.getWeights().getRow(1));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, new NeuronID(1, 0), new NeuronID(1, 0));
    }

    @Test
    public void testAddConnectionInputAlreadyExists() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = (NeuralNetworkLayerImpl) nn.getLayer(1);
        NeuralNetworkLayer outputLayer = nn.getLayer(2);

        hiddenLayer.modify()
                .addConnection(outputLayer.getNeuron(0), hiddenLayer.getNeuron(0), 1)
                .addConnection(outputLayer.getNeuron(0), hiddenLayer.getNeuron(1), 1);

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeuron(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(outputLayer.getNeuron(0)));

        // check weights
        assertEquals(2, hiddenLayer.getWeights().getRowDimension());
        assertEquals(2, hiddenLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, 1}, hiddenLayer.getWeights().getRow(0));
        assertArrayEquals(new double[]{-1, 1}, hiddenLayer.getWeights().getRow(1));
    }

    @Test
    public void testAddConnectionError() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayer outputLayer = nn.getLayer(2);

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> hiddenLayer.modify()
                .addConnection(hiddenLayer.getNeuron(0), outputLayer.getNeuron(0), 1));
        assertEquals("Can't add a connection to neuron NeuronID[layerID=2, neuronID=0] in layer 1", e1.getMessage());

        IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> outputLayer.modify()
                .addConnection(hiddenLayer.getNeuron(0), outputLayer.getNeuron(0), 1));
        assertEquals("The connection from neuron NeuronID[layerID=1, neuronID=0] to NeuronID[layerID=2, neuronID=0] already exists", e2.getMessage());
    }

    @Test
    public void testRemoveConnectionAndInputNeuron() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer inputLayer = nn.getLayer(0);
        NeuralNetworkLayer hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = (NeuralNetworkLayerImpl) nn.getLayer(2);

        outputLayer.modify()
                .addConnection(inputLayer.getNeuron(0), outputLayer.getNeuron(0), 1)
                .removeConnection(new NeuronID(0, 0), new NeuronID(2, 0));

        // check input neurons
        assertEquals(2, outputLayer.getInputNeurons().size());
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeuron(0)));
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeuron(1)));

        // check weights
        assertEquals(1, outputLayer.getWeights().getRowDimension());
        assertEquals(2, outputLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, -1}, outputLayer.getWeights().getRow(0));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, null, null);
    }

    @Test
    public void testRemoveConnectionAndKeepInputNeuron() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayer inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = (NeuralNetworkLayerImpl) nn.getLayer(1);

        hiddenLayer.modify()
                .addConnection(hiddenLayer.getNeuron(0), hiddenLayer.getNeuron(0), 1)
                .addConnection(hiddenLayer.getNeuron(0), hiddenLayer.getNeuron(1), 1)
                .removeConnection(new NeuronID(1, 0), new NeuronID(1, 0));

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(hiddenLayer.getNeuron(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeuron(0)));

        // check weights
        assertEquals(2, hiddenLayer.getWeights().getRowDimension());
        assertEquals(2, hiddenLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, 0}, hiddenLayer.getWeights().getRow(0));
        assertArrayEquals(new double[]{-1, 1}, hiddenLayer.getWeights().getRow(1));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, new NeuronID(1, 0), new NeuronID(1, 1));
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    private void checkNumberOfNeurons(int expectedNumber, NeuralNetworkLayer layer) {
        assertEquals(expectedNumber, layer.getNumberOfNeurons());
        assertEquals(expectedNumber, layer.getBias().getDimension());
        assertEquals(expectedNumber, layer.getActivation().getDimension());
        assertEquals(expectedNumber, layer.getWeights().getRowDimension());
    }

    /**
     * check if the new connection with weight 1 and nothing else was added to the neural network (fully connected 1-2-1)
     */
    private void checkIsOneTwoOneNetWithAdditionalConnection(NeuralNetwork nn, NeuronID start, NeuronID end) {
        // check weight matrix of input layer
        NeuralNetworkLayerImpl input = (NeuralNetworkLayerImpl) nn.getLayer(0);
        assertEquals(0, input.getInputNeurons().size());

        // check weight matrix of hidden layer
        NeuralNetworkLayerImpl hidden = (NeuralNetworkLayerImpl) nn.getLayer(1);
        assertEquals(end != null && end.layerID() == 1 ? 2 : 1, hidden.getInputNeurons().size());
        assertEquals(2, hidden.getWeights().getRowDimension());
        assertEquals(end != null && end.layerID() == 1 ? 2 : 1, hidden.getWeights().getColumnDimension());

        // check weight matrix of output layer
        NeuralNetworkLayerImpl output = (NeuralNetworkLayerImpl) nn.getLayer(2);
        assertEquals(end != null && end.layerID() == 2 ? 3 : 2, output.getInputNeurons().size());
        assertEquals(1, output.getWeights().getRowDimension());
        assertEquals(end != null && end.layerID() == 2 ? 3 : 2, output.getWeights().getColumnDimension());

        // check neuron of input layer
        checkExpectedIncomingConnectionsAndEventuallyNewOne(start, end, input.getNeuron(0));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(start, end, input.getNeuron(0), new NeuronID(1, 0), new NeuronID(1, 1));

        // check neurons of hidden layer
        checkExpectedIncomingConnectionsAndEventuallyNewOne(start, end, hidden.getNeuron(0), new NeuronID(0, 0));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(start, end, hidden.getNeuron(0), new NeuronID(2, 0));
        checkExpectedIncomingConnectionsAndEventuallyNewOne(start, end, hidden.getNeuron(1), new NeuronID(0, 0));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(start, end, hidden.getNeuron(1), new NeuronID(2, 0));

        // check neuron of output layer
        checkExpectedIncomingConnectionsAndEventuallyNewOne(start, end, output.getNeuron(0), new NeuronID(1, 0), new NeuronID(1, 1));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(start, end, output.getNeuron(0));
    }

    private void checkExpectedIncomingConnectionsAndEventuallyNewOne(NeuronID start, NeuronID end, Neuron neuron, NeuronID... incoming) {
        assertEquals(incoming.length + (neuron.getNeuronID().equals(end) ? 1 : 0), neuron.getIncomingConnections().size());
        // check expected incoming connections exist
        for (NeuronID in : incoming) {
            assertEquals(1, neuron.getIncomingConnections()
                    .stream()
                    .filter(c -> c.start().getNeuronID().equals(in))
                    .filter(c -> c.getWeight() == -1)
                    .count(), String.format("Expected connection from %s to %s is missing", in, neuron.getNeuronID()));
        }
        // if this neuron is the target of the new connection check if the new connection is registered as incoming
        if (neuron.getNeuronID().equals(end)) {
            assertEquals(1, neuron.getIncomingConnections()
                    .stream()
                    .filter(c -> c.start().getNeuronID().equals(start))
                    .filter(c -> c.getWeight() == 1)
                    .count(), String.format("Expected connection from %s to %s is missing", start, neuron.getNeuronID()));
        }
    }

    private void checkExpectedOutgoingConnectionsAndEventuallyNewOne(NeuronID start, NeuronID end, Neuron neuron, NeuronID... outgoing) {
        assertEquals(outgoing.length + (neuron.getNeuronID().equals(start) ? 1 : 0), neuron.getOutgoingConnections().size());
        // check expected outgoing connections exist
        for (NeuronID out : outgoing) {
            assertEquals(1, neuron.getOutgoingConnections()
                    .stream()
                    .filter(c -> c.end().getNeuronID().equals(out))
                    .filter(c -> c.getWeight() == -1)
                    .count(), String.format("Expected connection from %s to %s is missing", neuron.getNeuronID(), out));
        }
        // if this neuron is the source of the new connection check if the new connection is registered as outgoing
        if (neuron.getNeuronID().equals(start)) {
            assertEquals(1, neuron.getOutgoingConnections()
                    .stream()
                    .filter(c -> c.end().getNeuronID().equals(end))
                    .filter(c -> c.getWeight() == 1)
                    .count(), String.format("Expected connection from %s to %s is missing", neuron.getNeuronID(), end));
        }
    }

}
