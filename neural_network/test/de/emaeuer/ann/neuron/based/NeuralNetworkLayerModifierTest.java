package de.emaeuer.ann.neuron.based;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkBuilderImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkLayerImpl;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetwork;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.configuration.ConfigurationHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkLayerModifierTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private NeuralNetworkLayerImpl buildHiddenLayer(int numberOfNeurons) {
        NeuronBasedNeuralNetwork nn = NeuronBasedNeuralNetworkBuilder.build()
                .inputLayer()
                .fullyConnectToNextLayer()
                .hiddenLayer(numberOfNeurons)
                .fullyConnectToNextLayer()
                .outputLayer()
                .finish();
        return null;
    }

    private NeuralNetworkImpl buildNeuralNetwork(int... numberOfNeurons) {
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
    public void testAddNeuron() {
        NeuralNetworkLayerImpl layer = buildHiddenLayer(1);

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
        NeuralNetworkImpl nn = buildNeuralNetwork(2, 2);

        UnsupportedOperationException e1 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(0).modify().addNeuron(0));
        assertEquals("Adding neurons to the input or output layer is not supported", e1.getMessage());

        UnsupportedOperationException e2 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(1).modify().addNeuron(0));
        assertEquals("Adding neurons to the input or output layer is not supported", e2.getMessage());
    }

    @Test
    public void testRemoveNeuron() {
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 3, 1);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = nn.getLayer(2);

        hiddenLayer.modify().removeNeuron(new NeuronID(hiddenLayer.getLayerIndex(), 0));

        checkNumberOfNeurons(2, hiddenLayer);

        // check remaining neurons were shifted
        assertEquals(0, hiddenLayer.getNeurons().get(0).getNeuronIndex());
        assertEquals(1, hiddenLayer.getNeurons().get(1).getNeuronIndex());
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
        NeuralNetworkImpl nn = buildNeuralNetwork(2, 1, 2);

        UnsupportedOperationException e1 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(0).modify().removeNeuron(new NeuronID(0, 0)));
        assertEquals("Removing neurons from the input or output layer is not supported", e1.getMessage());

        UnsupportedOperationException e2 = assertThrows(UnsupportedOperationException.class, () -> nn.getLayer(2).modify().removeNeuron(new NeuronID(2, 0)));
        assertEquals("Removing neurons from the input or output layer is not supported", e2.getMessage());

        IllegalStateException e3 = assertThrows(IllegalStateException.class, () -> nn.getLayer(1).modify().removeNeuron(new NeuronID(1, 0)));
        assertEquals("Removing the neuron isn't possible because it is the last one in this layer. Delete the layer instead", e3.getMessage());
    }

    @Test
    public void testAddForwardConnection() {
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = nn.getLayer(2);

        outputLayer.modify().addConnection(new NeuronID(0, 0), new NeuronID(2, 0), 1);

        // check input neurons
        assertEquals(3, outputLayer.getInputNeurons().size());
        assertTrue(outputLayer.getInputNeurons().contains(inputLayer.getNeurons().get(0)));
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeurons().get(0)));
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeurons().get(1)));

        // check weights
        assertEquals(1, outputLayer.getWeights().getRowDimension());
        assertEquals(3, outputLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, -1, 1}, outputLayer.getWeights().getRow(0));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, new NeuronID(0, 0), new NeuronID(2, 0));
    }

    @Test
    public void testAddLateralConnection() {
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl layer = nn.getLayer(1);

        layer.modify().addConnection(new NeuronID(1, 0), new NeuronID(1, 1), 1);

        // check input neurons
        assertEquals(2, layer.getInputNeurons().size());
        assertTrue(layer.getInputNeurons().contains(inputLayer.getNeurons().get(0)));
        assertTrue(layer.getInputNeurons().contains(layer.getNeurons().get(0)));

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
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = nn.getLayer(2);

        hiddenLayer.modify().addConnection(new NeuronID(2, 0), new NeuronID(1, 0), 1);

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeurons().get(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(outputLayer.getNeurons().get(0)));

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
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);

        hiddenLayer.modify().addConnection(new NeuronID(1, 0), new NeuronID(1, 0), 1);

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeurons().get(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(hiddenLayer.getNeurons().get(0)));

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
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = nn.getLayer(2);

        hiddenLayer.modify()
                .addConnection(new NeuronID(2, 0), new NeuronID(1, 0), 1)
                .addConnection(new NeuronID(2, 0), new NeuronID(1, 1), 1);

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeurons().get(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(outputLayer.getNeurons().get(0)));

        // check weights
        assertEquals(2, hiddenLayer.getWeights().getRowDimension());
        assertEquals(2, hiddenLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, 1}, hiddenLayer.getWeights().getRow(0));
        assertArrayEquals(new double[]{-1, 1}, hiddenLayer.getWeights().getRow(1));
    }

    @Test
    public void testAddConnectionError() {
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = nn.getLayer(2);

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> hiddenLayer.modify()
                .addConnection(new NeuronID(1, 0), new NeuronID(2, 0), 1));
        assertEquals("Can't add a connection to neuron NeuronID[layerID=2, neuronID=0] in layer 1", e1.getMessage());

        IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> outputLayer.modify()
                .addConnection(new NeuronID(1, 0), new NeuronID(2, 0), 1));
        assertEquals("The connection from neuron NeuronID[layerID=1, neuronID=0] to NeuronID[layerID=2, neuronID=0] already exists", e2.getMessage());
    }

    @Test
    public void testRemoveConnectionAndInputNeuron() {
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);
        NeuralNetworkLayerImpl outputLayer = nn.getLayer(2);

        outputLayer.modify()
                .addConnection(new NeuronID(0, 0), new NeuronID(2, 0), 1)
                .removeConnection(new NeuronID(0, 0), new NeuronID(2, 0));

        // check input neurons
        assertEquals(2, outputLayer.getInputNeurons().size());
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeurons().get(0)));
        assertTrue(outputLayer.getInputNeurons().contains(hiddenLayer.getNeurons().get(1)));

        // check weights
        assertEquals(1, outputLayer.getWeights().getRowDimension());
        assertEquals(2, outputLayer.getWeights().getColumnDimension());
        assertArrayEquals(new double[]{-1, -1}, outputLayer.getWeights().getRow(0));

        // check neurons
        checkIsOneTwoOneNetWithAdditionalConnection(nn, null, null);
    }

    @Test
    public void testRemoveConnectionAndKeepInputNeuron() {
        NeuralNetworkImpl nn = buildNeuralNetwork(1, 2, 1);
        NeuralNetworkLayerImpl inputLayer = nn.getLayer(0);
        NeuralNetworkLayerImpl hiddenLayer = nn.getLayer(1);

        hiddenLayer.modify()
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 0), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 1), 1)
                .removeConnection(new NeuronID(1, 0), new NeuronID(1, 0));

        // check input neurons
        assertEquals(2, hiddenLayer.getInputNeurons().size());
        assertTrue(hiddenLayer.getInputNeurons().contains(hiddenLayer.getNeurons().get(0)));
        assertTrue(hiddenLayer.getInputNeurons().contains(inputLayer.getNeurons().get(0)));

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

    private void checkNumberOfNeurons(int expectedNumber, NeuralNetworkLayerImpl layer) {
        assertEquals(expectedNumber, layer.getNumberOfNeurons());
        assertEquals(expectedNumber, layer.getBias().getDimension());
        assertEquals(expectedNumber, layer.getActivation().getDimension());
        assertEquals(expectedNumber, layer.getWeights().getRowDimension());
    }

    /**
     * check if the new connection with weight 1 and nothing else was added to the neural network (fully connected 1-2-1)
     */
    private void checkIsOneTwoOneNetWithAdditionalConnection(NeuralNetworkImpl nn, NeuronID start, NeuronID end) {
        // check weight matrix of input layer
        NeuralNetworkLayerImpl input = nn.getLayer(0);
        assertEquals(0, input.getInputNeurons().size());

        // check weight matrix of hidden layer
        NeuralNetworkLayerImpl hidden = nn.getLayer(1);
        assertEquals(end != null && end.getLayerIndex() == 1 ? 2 : 1, hidden.getInputNeurons().size());
        assertEquals(2, hidden.getWeights().getRowDimension());
        assertEquals(end != null && end.getLayerIndex() == 1 ? 2 : 1, hidden.getWeights().getColumnDimension());

        // check weight matrix of output layer
        NeuralNetworkLayerImpl output = nn.getLayer(2);
        assertEquals(end != null && end.getLayerIndex() == 2 ? 3 : 2, output.getInputNeurons().size());
        assertEquals(1, output.getWeights().getRowDimension());
        assertEquals(end != null && end.getLayerIndex() == 2 ? 3 : 2, output.getWeights().getColumnDimension());

        // check neuron of input layer
        checkExpectedIncomingConnectionsAndEventuallyNewOne(nn, start, end, input.getNeurons().get(0));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(nn, start, end, input.getNeurons().get(0), new NeuronID(1, 0), new NeuronID(1, 1));

        // check neurons of hidden layer
        checkExpectedIncomingConnectionsAndEventuallyNewOne(nn, start, end, hidden.getNeurons().get(0), new NeuronID(0, 0));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(nn, start, end, hidden.getNeurons().get(0), new NeuronID(2, 0));
        checkExpectedIncomingConnectionsAndEventuallyNewOne(nn, start, end, hidden.getNeurons().get(1), new NeuronID(0, 0));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(nn, start, end, hidden.getNeurons().get(1), new NeuronID(2, 0));

        // check neuron of output layer
        checkExpectedIncomingConnectionsAndEventuallyNewOne(nn, start, end, output.getNeurons().get(0), new NeuronID(1, 0), new NeuronID(1, 1));
        checkExpectedOutgoingConnectionsAndEventuallyNewOne(nn, start, end, output.getNeurons().get(0));
    }

    private void checkExpectedIncomingConnectionsAndEventuallyNewOne(NeuralNetworkImpl nn, NeuronID start, NeuronID end, NeuronID neuron, NeuronID... incoming) {
        assertEquals(incoming.length + (neuron.equals(end) ? 1 : 0), nn.getIncomingConnectionsOfNeuron(neuron).size());
        // check expected incoming connections exist
        for (NeuronID in : incoming) {
            assertEquals(1, nn.getIncomingConnectionsOfNeuron(neuron)
                    .stream()
                    .filter(n -> n.equals(in))
                    .filter(n -> nn.getWeightOfConnection(n, neuron) == -1)
                    .count(), String.format("Expected connection from %s to %s is missing", in, neuron));
        }
        // if this neuron is the target of the new connection check if the new connection is registered as incoming
        if (neuron.equals(end)) {
            assertEquals(1, nn.getIncomingConnectionsOfNeuron(neuron)
                    .stream()
                    .filter(n -> n.equals(start))
                    .filter(n -> nn.getWeightOfConnection(n, neuron) == 1)
                    .count(), String.format("Expected connection from %s to %s is missing", start, neuron));
        }
    }

    private void checkExpectedOutgoingConnectionsAndEventuallyNewOne(NeuralNetworkImpl nn, NeuronID start, NeuronID end, NeuronID neuron, NeuronID... outgoing) {
        assertEquals(outgoing.length + (neuron.equals(start) ? 1 : 0), nn.getOutgoingConnectionsOfNeuron(neuron).size());
        // check expected outgoing connections exist
        for (NeuronID out : outgoing) {
            assertEquals(1, nn.getOutgoingConnectionsOfNeuron(neuron)
                    .stream()
                    .filter(n -> n.equals(out))
                    .filter(n -> nn.getWeightOfConnection(neuron, n) == -1)
                    .count(), String.format("Expected connection from %s to %s is missing", neuron, out));
        }
        // if this neuron is the source of the new connection check if the new connection is registered as outgoing
        if (neuron.equals(start)) {
            assertEquals(1, nn.getOutgoingConnectionsOfNeuron(neuron)
                    .stream()
                    .filter(n -> n.equals(end))
                    .filter(n -> nn.getWeightOfConnection(neuron, n) == 1)
                    .count(), String.format("Expected connection from %s to %s is missing", neuron, end));
        }
    }

}
