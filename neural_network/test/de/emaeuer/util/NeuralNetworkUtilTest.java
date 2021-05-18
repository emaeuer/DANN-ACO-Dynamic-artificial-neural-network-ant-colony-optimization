package de.emaeuer.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkBuilder;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetwork;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.ann.util.MathUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil.Connection;
import de.emaeuer.configuration.ConfigurationHandler;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkUtilTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private NeuralNetwork buildNeuralNetwork(int... numberOfNeurons) {
        if (numberOfNeurons.length < 2) {
            fail("A neural network needs at least 2 layers");
        }

        NeuralNetworkBuilder<?> builder = NeuralNetwork.build()
                .inputLayer(numberOfNeurons[0])
                .fullyConnectToNextLayer();

        for (int i = 1; i < numberOfNeurons.length - 1; i++) {
            builder = builder.hiddenLayer(numberOfNeurons[i])
                    .fullyConnectToNextLayer();
        }

        return builder.outputLayer(numberOfNeurons[numberOfNeurons.length - 1])
                .finish();
    }

    private NeuralNetwork buildNeuronBasedNeuralNetwork(int... numberOfNeurons) {
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

        return builder.outputLayer().finish();
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testIterateNeuralNetwork() {
        NeuralNetwork nn = buildNeuralNetwork(3, 2, 3);

        Set<Connection> expectedConnections = new HashSet<>();
        expectedConnections.add(new Connection(new NeuronID(0, 0), new NeuronID(1, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 0), new NeuronID(1, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 1), new NeuronID(1, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 1), new NeuronID(1, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 2), new NeuronID(1, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 2), new NeuronID(1, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 0), new NeuronID(2, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 0), new NeuronID(2, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 0), new NeuronID(2, 2), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 1), new NeuronID(2, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 1), new NeuronID(2, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 1), new NeuronID(2, 2), 0));

        Iterator<Connection> actualIterator = NeuralNetworkUtil.iterateNeuralNetworkConnections(nn);

        while (actualIterator.hasNext()) {
            Connection next = actualIterator.next();
            assertNotNull(next);
            assertTrue(expectedConnections.remove(next));
        }

        assertTrue(expectedConnections.isEmpty(), expectedConnections.toString());
    }

    @Test
    public void testIterateNeuronBasedNeuralNetwork() {
        NeuralNetwork nn = buildNeuronBasedNeuralNetwork(3, 2, 3);

        Set<Connection> expectedConnections = new HashSet<>();
        expectedConnections.add(new Connection(new NeuronID(0, 0), new NeuronID(1, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 0), new NeuronID(1, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 1), new NeuronID(1, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 1), new NeuronID(1, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 2), new NeuronID(1, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(0, 2), new NeuronID(1, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 0), new NeuronID(2, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 0), new NeuronID(2, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 0), new NeuronID(2, 2), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 1), new NeuronID(2, 0), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 1), new NeuronID(2, 1), 0));
        expectedConnections.add(new Connection(new NeuronID(1, 1), new NeuronID(2, 2), 0));

        Iterator<Connection> actualIterator = NeuralNetworkUtil.iterateNeuralNetworkConnections(nn);

        while (actualIterator.hasNext()) {
            Connection next = actualIterator.next();
            assertNotNull(next);
            assertTrue(expectedConnections.remove(next), String.format("Connection from %s to %s shouldn't exist", next.start(), next.end()));
        }

        assertTrue(expectedConnections.isEmpty(), expectedConnections.toString());
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
