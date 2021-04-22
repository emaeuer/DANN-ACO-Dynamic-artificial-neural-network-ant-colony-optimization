package de.emaeuer.paco.pheromone;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkBuilder;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.pheromone.FitnessPopulationBasedPheromone;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.POPULATION_SIZE;
import static org.junit.jupiter.api.Assertions.*;

public class PopulationBasedPheromoneTest {

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

    private ConfigurationHandler<PacoConfiguration> buildConfiguration(int populationSize) {
        ConfigurationHandler<PacoConfiguration> handler = new ConfigurationHandler<>(PacoConfiguration.class);
        handler.setValue(POPULATION_SIZE, populationSize);
        return handler;
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testAddSolutionsPopulationSizeNotReached() {
        FitnessPopulationBasedPheromone pheromone = new FitnessPopulationBasedPheromone(buildConfiguration(2), buildNeuralNetwork(2, 2));

        // create test data
        NeuralNetwork nnA = buildNeuralNetwork(2, 2);
        nnA.modify()
                .removeConnection(new NeuronID(0, 0), new NeuronID(1, 1))
                .removeConnection(new NeuronID(0, 1), new NeuronID(1, 0))
                .setWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0), 1)
                .setWeightOfConnection(new NeuronID(0, 1), new NeuronID(1, 1), 2)
                .setBiasOfNeuron(new NeuronID(1, 0), 3)
                .setBiasOfNeuron(new NeuronID(1, 1), 4);

        PacoAnt antA = new PacoAnt(nnA);
        antA.setFitness(100);

        NeuralNetwork nnB = buildNeuralNetwork(2, 2);
        nnB.modify()
                .removeConnection(new NeuronID(0, 1), new NeuronID(1, 0))
                .setWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0), -1)
                .setWeightOfConnection(new NeuronID(0, 1), new NeuronID(1, 1), -2)
                .setWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 1), -3)
                .setBiasOfNeuron(new NeuronID(1, 0), -4)
                .setBiasOfNeuron(new NeuronID(1, 1), -5);
        PacoAnt antB = new PacoAnt(nnB);
        antB.setFitness(90);

        // call method to test
        pheromone.addAntToPopulation(antA);
        pheromone.addAntToPopulation(antB);

        // evaluate method call
        // check ants were successfully added to the population
        List<PacoAnt> population = new ArrayList<>(pheromone.getPopulation());
        assertSame(antA, population.get(1));
        assertSame(antB, population.get(0));

        // check weight pheromone was updated
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 0), new NeuronID(1, 0), nnA),
                1.0, -1.0);
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 0), new NeuronID(1, 1), nnA),
                 -3.0);
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 1), new NeuronID(1, 0), nnA));
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 1), new NeuronID(1, 1), nnA),
                2.0, -2.0);
    }

    @Test
    public void testAddSolutionsPopulationSizeReached() {
        FitnessPopulationBasedPheromone pheromone = new FitnessPopulationBasedPheromone(buildConfiguration(2), buildNeuralNetwork(2, 2));

        // create test data
        NeuralNetwork nnA = buildNeuralNetwork(2, 2);
        nnA.modify()
                .removeConnection(new NeuronID(0, 0), new NeuronID(1, 1))
                .removeConnection(new NeuronID(0, 1), new NeuronID(1, 0))
                .setWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0), 1)
                .setWeightOfConnection(new NeuronID(0, 1), new NeuronID(1, 1), 2)
                .setBiasOfNeuron(new NeuronID(1, 0), 3)
                .setBiasOfNeuron(new NeuronID(1, 1), 4);

        PacoAnt antA = new PacoAnt(nnA);
        antA.setFitness(100);

        NeuralNetwork nnB = buildNeuralNetwork(2, 2);
        nnB.modify()
                .removeConnection(new NeuronID(0, 1), new NeuronID(1, 0))
                .setWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0), -1)
                .setWeightOfConnection(new NeuronID(0, 1), new NeuronID(1, 1), -2)
                .setWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 1), -3)
                .setBiasOfNeuron(new NeuronID(1, 0), -4)
                .setBiasOfNeuron(new NeuronID(1, 1), -5);
        PacoAnt antB = new PacoAnt(nnB);
        antB.setFitness(90);

        NeuralNetwork nnC = buildNeuralNetwork(2, 2);
        nnC.modify()
                .removeConnection(new NeuronID(0, 0), new NeuronID(1, 1))
                .setWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0), 10)
                .setWeightOfConnection(new NeuronID(0, 1), new NeuronID(1, 1), 11)
                .setWeightOfConnection(new NeuronID(0, 1), new NeuronID(1, 0), 12)
                .setBiasOfNeuron(new NeuronID(1, 0), 13)
                .setBiasOfNeuron(new NeuronID(1, 1), 14);
        PacoAnt antC = new PacoAnt(nnC);
        antC.setFitness(110);

        // call method to test
        pheromone.addAntToPopulation(antA);
        pheromone.addAntToPopulation(antB);
        pheromone.addAntToPopulation(antC);

        // evaluate method call
        // check ants were successfully added to the population
        List<PacoAnt> population = new ArrayList<>(pheromone.getPopulation());
        assertSame(antC, population.get(1));
        assertSame(antA, population.get(0));

        // check weight pheromone was updated
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 0), new NeuronID(1, 0), nnA),
                1.0, 10.0);
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 0), new NeuronID(1, 1), nnA));
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 1), new NeuronID(1, 0), nnA),
                12.0);
        assertContainsAll(pheromone.getPopulationValues(new NeuronID(0, 1), new NeuronID(1, 1), nnA),
                2.0, 11.0);
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    private void assertContainsAll(Collection<Double> actualValues, Double... expectedValues) {
        if (expectedValues.length == 0) {
            assertNull(actualValues);
            return;
        }
        assertEquals(expectedValues.length, actualValues.size());

        for (double expectedValue : expectedValues) {
            assertTrue(actualValues.contains(expectedValue), String.format("Expected value %s was not found", expectedValue));
        }
    }

}
