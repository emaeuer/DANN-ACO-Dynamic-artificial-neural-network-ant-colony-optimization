package de.emaeuer.aco.pheromone;

import de.emaeuer.aco.pheromone.impl.PheromoneMatrixLayer;
import de.emaeuer.ann.*;
import de.emaeuer.ann.NeuronID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PheromoneMatrixTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private static final double PHEROMONE_VALUE = 0.1;

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
    public void testFullyConnectedCreation() {
        NeuralNetwork nn = buildNeuralNetwork(2, 3);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn);

        // test for each neural network layer one pheromone matrix layer was created
        assertEquals(nn.getDepth(), matrix.getNumberOfLayers());

        // check all connections were initialized with the initial pheromone value
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 3, new NeuronID(1, 0), new NeuronID(1, 1), new NeuronID(1, 2));
        checkHasConnectionsTo(matrix, new NeuronID(0, 1), 3, new NeuronID(1, 0), new NeuronID(1, 1), new NeuronID(1, 2));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 0);
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 0);
        checkHasConnectionsTo(matrix, new NeuronID(1, 2), 0);

        // check all bias were initialized with the initial pheromone value
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 2)));
    }

    @Test
    public void testPartiallyConnectedCreation() {
        NeuralNetwork nn = NeuralNetwork.build()
                .inputLayer(2)
                .outputLayer(b -> b.numberOfNeurons(2)
                        .addConnection(new NeuronID(0, 0), new NeuronID(1, 0), 0)
                        .addConnection(new NeuronID(0, 1), new NeuronID(1, 1), 0))
                .finish();
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn);

        // test for each neural network layer one pheromone matrix layer was created
        assertEquals(nn.getDepth(), matrix.getNumberOfLayers());

        // check all connections were initialized with the initial pheromone value
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 2, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(0, 1), 2, new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 0);
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 0);

        // check all bias were initialized with the initial pheromone value
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
    }

    @Test
    public void testDifferentConnectionTypesCreation() {
        NeuralNetwork nn = NeuralNetwork.build()
                .inputLayer(2)
                .hiddenLayer(b -> b.numberOfNeurons(2)
                        .addConnection(new NeuronID(0, 0), new NeuronID(1, 0), 0)
                        .addConnection(new NeuronID(0, 1), new NeuronID(1, 1), 0)) // lateral connection
                .outputLayer(b -> b.numberOfNeurons(2)
                        .addConnection(new NeuronID(1, 0), new NeuronID(2, 0), 0)
                        .addConnection(new NeuronID(1, 1), new NeuronID(2, 1), 0)
                        .addConnection(new NeuronID(0, 0), new NeuronID(2, 0), 0)) // skip connection
                .finish();

        nn.modify()
                .addConnection(new NeuronID(2, 1), new NeuronID(1, 1), 0) // recurrent connection
                .addConnection(new NeuronID(1, 1), new NeuronID(1, 1), 0); // direct recurrent connection

        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn);

        // test for each neural network layer one pheromone matrix layer was created
        assertEquals(nn.getDepth(), matrix.getNumberOfLayers());

        // check all connections were initialized with the initial pheromone value
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 3, new NeuronID(1, 0), new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(0, 1), 3, new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 3, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 3, new NeuronID(2, 1), new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 1);
        checkHasConnectionsTo(matrix, new NeuronID(2, 1), 1, new NeuronID(1, 1));

        // check all bias were initialized with the initial pheromone value
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 1)));
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    private void checkHasConnectionsTo(PheromoneMatrix matrix, NeuronID start, int expectedRowLength, NeuronID... targets) {
        PheromoneMatrixLayer layer = matrix.getLayer(start.getLayerIndex());
        double[] expectedRow = new double[expectedRowLength];

        for (NeuronID target : targets) {
            int targetIndex = layer.indexOfTarget(target);
            assertNotEquals(-1, targetIndex, String.format("Missing connection from %s to %s", start, target));
            expectedRow[targetIndex] = PHEROMONE_VALUE;
        }

        if (expectedRowLength == 0) {
            assertNull(matrix.getWeightPheromoneOfNeuron(start));
        } else {
            assertArrayEquals(expectedRow, matrix.getWeightPheromoneOfNeuron(start).toArray());
        }
    }
}
