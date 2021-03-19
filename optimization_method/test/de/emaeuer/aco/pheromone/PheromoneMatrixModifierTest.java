package de.emaeuer.aco.pheromone;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.aco.configuration.AcoConfiguration;
import de.emaeuer.optimization.aco.pheromone.PheromoneMatrix;
import de.emaeuer.optimization.aco.pheromone.impl.PheromoneMatrixLayer;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkBuilder;
import de.emaeuer.ann.NeuronID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PheromoneMatrixModifierTest {

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

    private ConfigurationHandler<AcoConfiguration> buildConfiguration() {
        return new ConfigurationHandler<>(AcoConfiguration.class);
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testSplitForwardConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());

        // modify neural network and pheromone matrix accordingly
        NeuronID start = new NeuronID(0, 0);
        NeuronID end = new NeuronID(1, 0);
        NeuronID intermediateNeuron = nn.modify()
                .splitConnection(start, end)
                .getLastModifiedNeuron();

        matrix.modify().splitConnection(start, end, intermediateNeuron);

        // assert same neurons
        assertSameNeurons(nn, matrix, 0, 0);
        assertSameNeurons(nn, matrix, 1, 0);
        assertSameNeurons(nn, matrix, 2, 0);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check has no connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 1, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 1, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 0);
    }

    @Test
    public void testSplitRecurrentConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);

        // modify neural network and pheromone matrix accordingly
        NeuronID start = new NeuronID(2, 0);
        NeuronID end = new NeuronID(1, 0);

        nn.modify().addConnection(start, end, 1);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());

        NeuronID intermediateNeuron = nn.modify()
                .splitConnection(start, end)
                .getLastModifiedNeuron();

        matrix.modify().splitConnection(start, end, intermediateNeuron);

        // assert same neurons
        assertSameNeurons(nn, matrix, 0, 0);
        assertSameNeurons(nn, matrix, 1, 0);
        assertSameNeurons(nn, matrix, 2, 0);
        assertSameNeurons(nn, matrix, 3, 0);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(3, 0)));

        // check has no connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 1, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 1, new NeuronID(3, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 1, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(3, 0), 1, new NeuronID(2, 0));
    }

    @Test
    public void testSplitLateralConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);

        // modify neural network and pheromone matrix accordingly
        NeuronID start = new NeuronID(1, 0);
        NeuronID end = new NeuronID(1, 1);

        nn.modify().addConnection(start, end, 1);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());

        NeuronID intermediateNeuron = nn.modify()
                .splitConnection(start, end)
                .getLastModifiedNeuron();

        matrix.modify().splitConnection(start, end, intermediateNeuron);

        // assert same neurons
        assertSameNeurons(nn, matrix, 0, 0);
        assertSameNeurons(nn, matrix, 1, 0);
        assertSameNeurons(nn, matrix, 1, 1);
        assertSameNeurons(nn, matrix, 1, 2);
        assertSameNeurons(nn, matrix, 2, 0);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 2)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check has no connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 2, new NeuronID(1, 0), new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 3, new NeuronID(2, 0), new NeuronID(1, 2));
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 3, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 2), 3, new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 0);
    }

    @Test
    public void testSplitDirectRecurrentConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);

        // modify neural network and pheromone matrix accordingly
        NeuronID start = new NeuronID(1, 0);
        NeuronID end = new NeuronID(1, 0);

        nn.modify().addConnection(start, end, 1);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());

        NeuronID intermediateNeuron = nn.modify()
                .splitConnection(start, end)
                .getLastModifiedNeuron();

        matrix.modify().splitConnection(start, end, intermediateNeuron);

        // assert same neurons
        assertSameNeurons(nn, matrix, 0, 0);
        assertSameNeurons(nn, matrix, 1, 0);
        assertSameNeurons(nn, matrix, 1, 1);
        assertSameNeurons(nn, matrix, 2, 0);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check has no connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 1, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 3, new NeuronID(2, 0), new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 3, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 0);
    }

    @Test
    public void testSplitSkipForwardConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);

        // modify neural network and pheromone matrix accordingly
        NeuronID start = new NeuronID(0, 0);
        NeuronID end = new NeuronID(2, 0);

        nn.modify().addConnection(start, end, 1);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());

        NeuronID intermediateNeuron = nn.modify()
                .splitConnection(start, end)
                .getLastModifiedNeuron();

        matrix.modify().splitConnection(start, end, intermediateNeuron);

        // assert same neurons
        assertSameNeurons(nn, matrix, 0, 0);
        assertSameNeurons(nn, matrix, 1, 0);
        assertSameNeurons(nn, matrix, 1, 1);
        assertSameNeurons(nn, matrix, 2, 0);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check has no connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 2, new NeuronID(1, 0), new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 1, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 1, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 0);
    }

    @Test
    public void testSplitSkipRecurrentConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1, 1);

        // modify neural network and pheromone matrix accordingly
        NeuronID start = new NeuronID(3, 0);
        NeuronID end = new NeuronID(1, 0);

        nn.modify().addConnection(start, end, 1);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());

        NeuronID intermediateNeuron = nn.modify()
                .splitConnection(start, end)
                .getLastModifiedNeuron();

        matrix.modify().splitConnection(start, end, intermediateNeuron);

        // assert same neurons
        assertSameNeurons(nn, matrix, 0, 0);
        assertSameNeurons(nn, matrix, 1, 0);
        assertSameNeurons(nn, matrix, 2, 0);
        assertSameNeurons(nn, matrix, 2, 1);
        assertSameNeurons(nn, matrix, 3, 0);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(3, 0)));

        // check has no connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 1, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 1, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 2, new NeuronID(3, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 1), 2, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(3, 0), 1, new NeuronID(2, 1));
    }

    @Test
    public void testAddNeuron() {
        NeuralNetwork nn = buildNeuralNetwork(1, 1, 1);
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());

        // modify neural network and pheromone matrix accordingly
        NeuronID lastAddedNeuron = nn.modify()
                .addNeuron(1, 1)
                .getLastModifiedNeuron();

        matrix.modify().addNeuron(lastAddedNeuron);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(lastAddedNeuron));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check has no connections
        checkHasConnectionsTo(matrix, lastAddedNeuron, 1);
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 1, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 1, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 0);
    }

    @Test
    public void testRemoveNeuron() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);

        // modify neural network and pheromone matrix accordingly
        NeuronID neuronToRemove = new NeuronID(1, 0);
        nn.modify()
                .addConnection(new NeuronID(1, 1), new NeuronID(1, 1), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 1), 1)
                .addConnection(new NeuronID(2, 0), new NeuronID(1, 0), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 0), 1);

        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());
        neuronToRemove = nn.modify()
                .removeNeuron(neuronToRemove)
                .getLastModifiedNeuron();
        matrix.modify().removeNeuron(neuronToRemove);

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 1, new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 2, new NeuronID(2, 0), new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 0);
    }

    @Test
    public void testAddConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);

        // modify neural network and pheromone matrix accordingly
        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());
        nn.modify()
                .addConnection(new NeuronID(1, 1), new NeuronID(1, 1), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 1), 1)
                .addConnection(new NeuronID(2, 0), new NeuronID(1, 0), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 0), 1);
        matrix.modify()
                .addConnection(new NeuronID(1, 1), new NeuronID(1, 1))
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 1))
                .addConnection(new NeuronID(2, 0), new NeuronID(1, 0))
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 0));

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 2, new NeuronID(1, 0), new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 3, new NeuronID(2, 0), new NeuronID(1, 1), new NeuronID(1, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 3, new NeuronID(2, 0), new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 1, new NeuronID(1, 0));
    }

    @Test
    public void testRemoveConnection() {
        NeuralNetwork nn = buildNeuralNetwork(1, 2, 1);

        // modify neural network and pheromone matrix accordingly
        nn.modify()
                .addConnection(new NeuronID(1, 1), new NeuronID(1, 1), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 1), 1)
                .addConnection(new NeuronID(2, 0), new NeuronID(1, 0), 1)
                .addConnection(new NeuronID(1, 0), new NeuronID(1, 0), 1);

        PheromoneMatrix matrix = PheromoneMatrix.buildForNeuralNetwork(nn, buildConfiguration());
        nn.modify()
                .removeConnection(new NeuronID(1, 1), new NeuronID(1, 1))
                .removeConnection(new NeuronID(1, 0), new NeuronID(1, 1))
                .removeConnection(new NeuronID(2, 0), new NeuronID(1, 0))
                .removeConnection(new NeuronID(1, 0), new NeuronID(1, 0));
        matrix.modify()
                .removeConnection(new NeuronID(1, 1), new NeuronID(1, 1))
                .removeConnection(new NeuronID(1, 0), new NeuronID(1, 1))
                .removeConnection(new NeuronID(2, 0), new NeuronID(1, 0))
                .removeConnection(new NeuronID(1, 0), new NeuronID(1, 0));

        // check bias
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(0, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 1)));
        assertEquals(PHEROMONE_VALUE, matrix.getBiasPheromoneOfNeuron(new NeuronID(2, 0)));

        // check connections
        checkHasConnectionsTo(matrix, new NeuronID(0, 0), 2, new NeuronID(1, 0), new NeuronID(1, 1));
        checkHasConnectionsTo(matrix, new NeuronID(1, 0), 1, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(1, 1), 1, new NeuronID(2, 0));
        checkHasConnectionsTo(matrix, new NeuronID(2, 0), 0);
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

    private void assertSameNeurons(NeuralNetwork nn, PheromoneMatrix matrix, int layerIndex, int inLayerIndex) {
        assertSame(nn.getNeuronsOfLayer(layerIndex).get(inLayerIndex), matrix.getLayer(layerIndex).getContainedNeurons().get(inLayerIndex));
    }

}
