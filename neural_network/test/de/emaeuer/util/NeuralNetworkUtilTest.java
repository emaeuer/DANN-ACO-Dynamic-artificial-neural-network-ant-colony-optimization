package de.emaeuer.util;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkBuilder;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.util.MathUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil.Connection;
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
            System.out.println(next);
            assertNotNull(next);
            assertTrue(expectedConnections.remove(next));
        }

        assertTrue(expectedConnections.isEmpty());
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
