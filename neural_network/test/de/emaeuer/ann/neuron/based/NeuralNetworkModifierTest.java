package de.emaeuer.ann.neuron.based;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkBuilderImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkLayerImpl;
import de.emaeuer.ann.impl.neuron.based.Neuron;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetwork;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;

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

        ConfigurationHandler<NeuralNetworkConfiguration> config = new ConfigurationHandler<>(NeuralNetworkConfiguration.class);
        config.setValue(NeuralNetworkConfiguration.INPUT_LAYER_SIZE, numberOfNeurons[0]);
        config.setValue(NeuralNetworkConfiguration.OUTPUT_LAYER_SIZE, numberOfNeurons[numberOfNeurons.length - 1]);

        NeuronBasedNeuralNetworkBuilder builder = NeuronBasedNeuralNetworkBuilder.buildWithConfiguration(config)
                .inputLayer()
                .fullyConnectToNextLayer();

        if (numberOfNeurons.length == 3) {
            builder.hiddenLayer(numberOfNeurons[1])
                    .fullyConnectToNextLayer();
        }

        NeuronBasedNeuralNetwork nn = builder.outputLayer().finish();

        // set all weights to -1 (for comparison with new weights)
        Iterator<NeuralNetworkUtil.Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(nn);
        while (connections.hasNext()) {
            NeuralNetworkUtil.Connection connection = connections.next();
            nn.modify().setWeightOfConnection(connection.start(), connection.end(), -1);
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

        nn.modify().splitConnection(new NeuronID(0, 0), new NeuronID(2, 0));

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

        NeuronID newNeuron = new NeuronID(1, 1);
        output = new NeuronID(2, 0);

        assertEquals(newNeuron, nn.modify().getLastModifiedNeuron());

        // check general updates
        assertEquals(3, nn.getDepth());
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
    public void testMultipleSplit() {
        NeuralNetwork nn = buildNeuralNetwork(4, 1);

        nn.modify()
                .splitConnection(new NeuronID(0, 0), new NeuronID(2, 0))
                .splitConnection(new NeuronID(0, 2), new NeuronID(2, 0))
                .splitConnection(new NeuronID(0, 2), new NeuronID(1, 1));

        // check general updates
        assertEquals(3, nn.getDepth());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(new NeuronID(0, 0)).size());
        assertEquals(0, nn.getIncomingConnectionsOfNeuron(new NeuronID(0, 0)).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(new NeuronID(0, 1)).size());
        assertEquals(0, nn.getIncomingConnectionsOfNeuron(new NeuronID(0, 1)).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(new NeuronID(0, 2)).size());
        assertEquals(0, nn.getIncomingConnectionsOfNeuron(new NeuronID(0, 2)).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(new NeuronID(0, 3)).size());
        assertEquals(0, nn.getIncomingConnectionsOfNeuron(new NeuronID(0, 3)).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(new NeuronID(1, 0)).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(new NeuronID(1, 0)).size());
        assertEquals(0, nn.getOutgoingConnectionsOfNeuron(new NeuronID(2, 0)).size());
        assertEquals(4, nn.getIncomingConnectionsOfNeuron(new NeuronID(2, 0)).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(new NeuronID(1, 1)).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(new NeuronID(1, 1)).size());
        assertEquals(1, nn.getOutgoingConnectionsOfNeuron(new NeuronID(1, 2)).size());
        assertEquals(1, nn.getIncomingConnectionsOfNeuron(new NeuronID(1, 2)).size());

        // check connection start and end
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(0, 0), new NeuronID(1, 0)));
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(0, 1), new NeuronID(2, 0)));
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(0, 2), new NeuronID(1, 2)));
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(0, 3), new NeuronID(2, 0)));
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(1, 0), new NeuronID(2, 0)));
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(1, 1), new NeuronID(2, 0)));
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(1, 2), new NeuronID(1, 1)));
    }

    @Test
    public void testAddNeuron() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1);

        nn.modify().addNeuron(1, 0.5);

        assertEquals(0.5, nn.getBiasOfNeuron(new NeuronID(1, 0)));

        assertEquals(1, nn.getNeuronsOfLayer(0).size());
        assertEquals(1, nn.getNeuronsOfLayer(1).size());
        assertEquals(1, nn.getNeuronsOfLayer(2).size());

        assertConnections(nn, new NeuronID(0, 0), new NeuronID(2, 0));
        assertConnections(nn, new NeuronID(1, 0));
        assertConnections(nn, new NeuronID(2, 0));
    }

    @Test
    public void testRemoveNeuron() {
        NeuralNetwork nn = buildNeuralNetwork(1, 3, 1);

        nn.modify()
                .addConnection(new NeuronID(2, 0), new NeuronID(1, 2), 1)
                .removeNeuron(new NeuronID(1, 1));

        assertEquals(1, nn.getNeuronsOfLayer(0).size());
        assertEquals(2, nn.getNeuronsOfLayer(1).size());
        assertEquals(1, nn.getNeuronsOfLayer(2).size());

        assertConnections(nn, new NeuronID(0, 0), new NeuronID(1, 0), new NeuronID(1, 1));
        assertConnections(nn, new NeuronID(1, 0), new NeuronID(2, 0));
        assertConnections(nn, new NeuronID(1, 1), new NeuronID(2, 0));
        assertConnections(nn, new NeuronID(2, 0), new NeuronID(1, 1));
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    public void assertConnections(NeuralNetwork nn, NeuronID source, NeuronID... targets) {
        assertEquals(targets.length, nn.getOutgoingConnectionsOfNeuron(source).size());

        for (NeuronID target : targets) {
            assertTrue(nn.getOutgoingConnectionsOfNeuron(source)
                    .stream()
                    .anyMatch(n -> n.equals(target)));
        }
    }

    private void checkExpectedIncomingConnectionsAndEventuallyNewOne(NeuralNetwork nn, NeuronID start, NeuronID end, NeuronID neuron, NeuronID... incoming) {
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

    private void checkExpectedOutgoingConnectionsAndEventuallyNewOne(NeuralNetwork nn, NeuronID start, NeuronID end, NeuronID neuron, NeuronID... outgoing) {
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
