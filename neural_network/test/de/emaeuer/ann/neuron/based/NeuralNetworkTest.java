package de.emaeuer.ann.neuron.based;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkBuilderImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkLayerImpl;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetwork;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.configuration.ConfigurationHandler;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkTest {

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
    public void testCopy() {
        NeuronBasedNeuralNetwork nn = (NeuronBasedNeuralNetwork) buildNeuralNetwork(2, 2, 2);

        // add all kinds of connections
        nn.modify()
                .addConnection(new NeuronID(2, 1), new NeuronID(1, 0), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 1), 1)
                .addConnection(new NeuronID(2, 0), new NeuronID(2, 0), 1);

        // copy neural network
        NeuronBasedNeuralNetwork copy = (NeuronBasedNeuralNetwork) nn.copy();

        // check general neural network
        assertNotSame(nn, copy);
        assertEquals(nn.getDepth(), copy.getDepth());

        List<NeuralNetworkUtil.Connection> expectedConnections = Arrays.asList(
                new NeuralNetworkUtil.Connection(new NeuronID(0, 0), new NeuronID(1, 0), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(0, 0), new NeuronID(1, 1), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(0, 1), new NeuronID(1, 0), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(0, 1), new NeuronID(1, 1), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(1, 0), new NeuronID(2, 0), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(1, 0), new NeuronID(2, 1), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(1, 1), new NeuronID(2, 0), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(1, 1), new NeuronID(2, 1), -1),
                new NeuralNetworkUtil.Connection(new NeuronID(2, 1), new NeuronID(1, 0), 1),
                new NeuralNetworkUtil.Connection(new NeuronID(1, 0), new NeuronID(1, 1), 1),
                new NeuralNetworkUtil.Connection(new NeuronID(2, 0), new NeuronID(2, 0), 1)
        );

        for (NeuralNetworkUtil.Connection connection : expectedConnections) {
            assertTrue(copy.neuronHasConnectionTo(connection.start(), connection.end()), String.format("Connection from %s to %s missing", connection.start(), connection.end()));
            assertEquals(connection.weight(), copy.getWeightOfConnection(connection.start(), connection.end()), String.format("Weights are different for connection %s to %s", connection.start(), connection.end()));
        }

        Iterator<NeuralNetworkUtil.Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(copy);
        int connectionCount = 0;
        while (connections.hasNext()) {
            connections.next();
            connectionCount++;
        }

        assertEquals(expectedConnections.size(), connectionCount);
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
