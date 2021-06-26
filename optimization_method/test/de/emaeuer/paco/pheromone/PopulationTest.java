package de.emaeuer.paco.pheromone;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.population.impl.AgeBasedPopulation;
import de.emaeuer.optimization.paco.population.impl.FitnessBasedPopulation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PopulationTest {

    private PacoAnt createAnt(double fitness) {
        PacoAnt ant = new PacoAnt(createBaseNetwork(), 0);
        ant.setFitness(fitness);
        return ant;
    }

    private ConfigurationHandler<PacoConfiguration> createConfiguration(int maxSize, boolean useElitism) {
        ConfigurationHandler<PacoConfiguration> config = new ConfigurationHandler<>(PacoConfiguration.class);
        config.setValue(PacoConfiguration.POPULATION_SIZE, maxSize);
        config.setValue(PacoConfiguration.ELITISM, useElitism);
        return config;
    }

    private NeuralNetwork createBaseNetwork() {
        ConfigurationHandler<NeuralNetworkConfiguration> config = new ConfigurationHandler<>(NeuralNetworkConfiguration.class);
        config.setValue(NeuralNetworkConfiguration.INPUT_LAYER_SIZE, 1);
        config.setValue(NeuralNetworkConfiguration.OUTPUT_LAYER_SIZE, 1);

        return NeuronBasedNeuralNetworkBuilder.buildWithConfiguration(config)
                .implicitBias()
                .inputLayer()
                .fullyConnectToNextLayer()
                .outputLayer()
                .finish();
    }

    @Test
    public void testAgeBasedWithoutElitism() {
        AgeBasedPopulation population = new AgeBasedPopulation(createConfiguration(5, false), createBaseNetwork(), null);

        List<PacoAnt> ants = new ArrayList<>();

        // add 5 ants (nothing should be removed)
        for (int i = 0; i < 5; i++) {
            PacoAnt ant = createAnt(5 - i);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            assertEquals(i + 1, population.getSize());
            assertFalse(population.removeAnt().isPresent(), "Failed in iteration " + i);
        }

        // add additional 5 ants (every step the oldest ant should be removed)
        for (int i = 0; i < 5; i++) {
            PacoAnt ant = createAnt(5 - i);
            assertSame(ant, population.addAnt(ant).orElse(null));
            PacoAnt removed = population.removeAnt().get();
            assertSame(removed, ants.remove(0));
            assertEquals(5, population.getSize());
        }

        assertTrue(ants.isEmpty());
    }

    @Test
    public void testAgeBasedWithElitism() {
        AgeBasedPopulation population = new AgeBasedPopulation(createConfiguration(5, true), createBaseNetwork(), null);

        List<PacoAnt> ants = new ArrayList<>();

        // add 4 ants (nothing should be removed)
        for (int i = 0; i < 4; i++) {
            PacoAnt ant = createAnt(i);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            assertEquals(i + 1, population.getSize());
            assertFalse(population.removeAnt().isPresent(), "Failed in iteration " + i);
        }

        PacoAnt globalBest = createAnt(10);
        population.addAnt(globalBest);

        // add additional 5 ants (every step the oldest ant should be removed)
        for (int i = 0; i < 5; i++) {
            PacoAnt ant = createAnt(5 - i);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            PacoAnt removed = population.removeAnt().get();
            assertSame(removed, ants.remove(0));
            assertEquals(5, population.getSize());
        }

        // test if the old global best is removed next because it has reached the maximum age and a better ant was added
        population.addAnt(createAnt(20));
        assertSame(globalBest, population.removeAnt().get());
    }

    @Test
    public void testFitnessBased() {
        FitnessBasedPopulation population = new FitnessBasedPopulation(createConfiguration(5, false), createBaseNetwork(), null);

        List<PacoAnt> ants = new ArrayList<>();

        // add 5 ants (nothing should be removed)
        for (int i = 0; i < 5; i++) {
            PacoAnt ant = createAnt(Math.pow(i + 1, 2) % 10 + 2);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            assertEquals(i + 1, population.getSize());
            assertFalse(population.removeAnt().isPresent(), "Failed in iteration " + i);
        }

        // check that ant is not added if the score is too low
        PacoAnt worstAnt = createAnt(1);
        assertFalse(population.addAnt(worstAnt).isPresent());
        assertFalse(population.removeAnt().isPresent());

        ants.sort(Comparator.comparingDouble(PacoAnt::getFitness));

        // add additional 5 ants which are better than the best 5 (every step the worst ant should be removed)
        for (int i = 0; i < 5; i++) {
            PacoAnt ant = createAnt(12 + i);
            assertSame(ant, population.addAnt(ant).orElse(null));
            PacoAnt removed = population.removeAnt().get();
            assertSame(removed, ants.remove(0));
            assertEquals(5, population.getSize());
        }

        assertTrue(ants.isEmpty());
    }

}
