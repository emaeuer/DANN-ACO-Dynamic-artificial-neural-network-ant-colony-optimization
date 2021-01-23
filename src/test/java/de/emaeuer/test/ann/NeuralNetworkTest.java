package de.emaeuer.test.ann;

import de.emaeuer.ann.*;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private NeuralNetwork createXorNetwork() {
        NeuralNetwork nn = new NeuralNetwork(2, 2, 1);
        NeuralNetworkModifier modifier = nn.getModifier();

        modifier.restartModification()
                .setWeightsOfNeuron(0, new ArrayRealVector(new double[]{1, -1}))
                .setWeightsOfNeuron(1, new ArrayRealVector(new double[]{-1, 1}))
                .finish()
                .modifyNextLayer()
                .setWeightsOfNeuron(0, new ArrayRealVector(new double[]{1}))
                .setWeightsOfNeuron(1, new ArrayRealVector(new double[]{1}))
                .finish();

        return nn;
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testCreation() {
        assertThrows(IllegalArgumentException.class, () -> new NeuralNetwork(1));

        NeuralNetwork network = new NeuralNetwork(1, 2, 3, 4);
        assertEquals(4, network.getNumberOfLayers());
        assertNotNull(network.getModifier());
        assertTrue(network.getInputLayer().isInputLayer());

        int expectedNumberOfNeurons = 1;
        for (NeuralNetworkLayer layer: network) {
            assertEquals(expectedNumberOfNeurons, layer.getNumberOfNeurons());
            if (!layer.isInputLayer()) {
                assertEquals(expectedNumberOfNeurons, layer.getBias().getDimension());
                assertEquals(expectedNumberOfNeurons - 1, layer.getWeights().getColumnDimension());
                assertEquals(expectedNumberOfNeurons, layer.getWeights().getRowDimension());
            } else {
                assertNull(layer.getBias());
                assertNull(layer.getWeights());
            }

            int neuronCounter = 0;
            for (Neuron neuron: layer) {
                assertEquals(new Neuron.NeuronID(expectedNumberOfNeurons - 1, neuronCounter), neuron.getIdentifier());
                assertEquals(neuronCounter, neuron.getIndexInLayer());
                if (!layer.isOutputLayer()) {
                    assertEquals(expectedNumberOfNeurons + 1, neuron.getConnections().size());
                } else {
                    assertEquals(0, neuron.getConnections().size());
                }
                neuronCounter++;
            }
            expectedNumberOfNeurons++;
        }
    }

    @Test
    public void testXOR() {
        NeuralNetwork nn = createXorNetwork();

        assertEquals(new ArrayRealVector(new double[]{0}), nn.process(new ArrayRealVector(new double[]{0, 0})));
        assertEquals(new ArrayRealVector(new double[]{1}), nn.process(new ArrayRealVector(new double[]{0, 1})));
        assertEquals(new ArrayRealVector(new double[]{1}), nn.process(new ArrayRealVector(new double[]{1, 0})));
        assertEquals(new ArrayRealVector(new double[]{0}), nn.process(new ArrayRealVector(new double[]{1, 1})));
    }

    @Test
    public void testNeuronManipulation() {
        NeuralNetwork nn = createXorNetwork();

        Neuron neuron = nn.getNeuron(new Neuron.NeuronID(1, 1));
        NeuralNetworkLayer layer = nn.getLayer(1);

        // changing the vector changes the neuron
        layer.getBias().setEntry(1, 3);
        assertEquals(3, neuron.getBias());
        assertEquals(3, layer.getBias().getEntry(1));
        // changing the neuron changes the vector
        neuron.setBias(5);
        assertEquals(5, neuron.getBias());
        assertEquals(5, layer.getBias().getEntry(1));
    }

    @Test
    public void testConnectionManipulation() {
        NeuralNetwork nn = createXorNetwork();

        NeuralNetworkLayer layer = nn.getLayer(1);
        Neuron startNeuron = nn.getNeuron(new Neuron.NeuronID(0, 1));
        Neuron endNeuron = nn.getNeuron(new Neuron.NeuronID(1, 0));
        Connection connection = startNeuron.getConnections().get(0);
        assertSame(endNeuron, connection.getEnd());

        // changing the matrix changes the connection
        layer.getWeights().setEntry(endNeuron.getIndexInLayer(), startNeuron.getIndexInLayer(), 3);
        assertEquals(3, layer.getWeights().getEntry(endNeuron.getIndexInLayer(), startNeuron.getIndexInLayer()));
        assertEquals(3, connection.getWeight());
        assertEquals(new ArrayRealVector(new double[]{3, 1}), startNeuron.getOutgoingWeights());

        // changing the connection changes the connection
        connection.setWeight(5);
        assertEquals(5, layer.getWeights().getEntry(endNeuron.getIndexInLayer(), startNeuron.getIndexInLayer()));
        assertEquals(5, connection.getWeight());
        assertEquals(new ArrayRealVector(new double[]{5, 1}), startNeuron.getOutgoingWeights());

    }

    @Test
    public void testCopy() {
        NeuralNetwork nn = createXorNetwork();
        NeuralNetwork copy = nn.copy();

        assertNotSame(copy, nn);

        NeuralNetworkLayer layer = nn.getInputLayer();
        NeuralNetworkLayer copyLayer = copy.getInputLayer();

        while (true) {
            assertEquals(layer.isInputLayer(), copyLayer.isInputLayer());
            assertEquals(layer.isOutputLayer(), copyLayer.isOutputLayer());
            assertEquals(layer.getWeights(), copyLayer.getWeights());
            assertEquals(layer.getBias(), copyLayer.getBias());
            neuronsEqual(layer, copyLayer, layer.isInputLayer(), layer.isOutputLayer());

            if (layer.isOutputLayer()) {
                break;
            } else {
                layer = layer.getNextLayer();
                copyLayer = copyLayer.getNextLayer();
            }
        }
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    private void neuronsEqual(NeuralNetworkLayer layer, NeuralNetworkLayer copyLayer, boolean isInput, boolean isOutput) {
        Iterator<Neuron> neurons = layer.iterator();
        Iterator<Neuron> copyNeurons = copyLayer.iterator();

        while (neurons.hasNext()) {
            Neuron neuron = neurons.next();
            Neuron copyNeuron = copyNeurons.next();

            assertEquals(neuron.getIdentifier(), copyNeuron.getIdentifier());
            assertEquals(neuron.getIndexInLayer(), copyNeuron.getIndexInLayer());
            connectionsEqual(neuron.getConnections(), copyNeuron.getConnections());

            if (!isInput) {
                assertEquals(neuron.getBias(), copyNeuron.getBias());
            }
            if (!isOutput) {
                assertEquals(neuron.getOutgoingWeights(), copyNeuron.getOutgoingWeights());
            }
        }

    }

    private void connectionsEqual(List<Connection> connections, List<Connection> copyConnections) {
        assertEquals(connections.size(), copyConnections.size());
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            Connection copyConnection = copyConnections.get(i);
            assertEquals(connection.getStart().getIdentifier(), copyConnection.getStart().getIdentifier());
            assertEquals(connection.getEnd().getIdentifier(), copyConnection.getEnd().getIdentifier());
            assertEquals(connection.getWeight(), copyConnection.getWeight());
        }
    }
}
