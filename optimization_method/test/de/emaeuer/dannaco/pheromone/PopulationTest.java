package de.emaeuer.dannaco.pheromone;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.optimization.dannaco.Ant;
import de.emaeuer.optimization.dannaco.configuration.DannacoConfiguration;
import de.emaeuer.optimization.dannaco.population.impl.AgeBasedPopulation;
import de.emaeuer.optimization.dannaco.population.impl.FitnessBasedPopulation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PopulationTest {

    private Ant createAnt(double fitness) {
        Ant ant = new Ant(createBaseNetwork(), 0);
        ant.setFitness(fitness);
        return ant;
    }

    private ConfigurationHandler<DannacoConfiguration> createConfiguration(int maxSize, boolean useElitism) {
        ConfigurationHandler<DannacoConfiguration> config = new ConfigurationHandler<>(DannacoConfiguration.class);
        config.setValue(DannacoConfiguration.POPULATION_SIZE, maxSize);
        config.setValue(DannacoConfiguration.ELITISM, useElitism);
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

        List<Ant> ants = new ArrayList<>();

        // add 5 ants (nothing should be removed)
        for (int i = 0; i < 5; i++) {
            Ant ant = createAnt(5 - i);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            assertEquals(i + 1, population.getSize());
            assertFalse(population.removeAnt().isPresent(), "Failed in iteration " + i);
        }

        // add additional 5 ants (every step the oldest ant should be removed)
        for (int i = 0; i < 5; i++) {
            Ant ant = createAnt(5 - i);
            assertSame(ant, population.addAnt(ant).orElse(null));
            Ant removed = population.removeAnt().get();
            assertSame(removed, ants.remove(0));
            assertEquals(5, population.getSize());
        }

        assertTrue(ants.isEmpty());
    }

    @Test
    public void testAgeBasedWithElitism() {
        AgeBasedPopulation population = new AgeBasedPopulation(createConfiguration(5, true), createBaseNetwork(), null);

        List<Ant> ants = new ArrayList<>();

        // add 4 ants (nothing should be removed)
        for (int i = 0; i < 4; i++) {
            Ant ant = createAnt(i);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            assertEquals(i + 1, population.getSize());
            assertFalse(population.removeAnt().isPresent(), "Failed in iteration " + i);
        }

        Ant globalBest = createAnt(10);
        population.addAnt(globalBest);

        // add additional 5 ants (every step the oldest ant should be removed)
        for (int i = 0; i < 5; i++) {
            Ant ant = createAnt(5 - i);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            Ant removed = population.removeAnt().get();
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

        List<Ant> ants = new ArrayList<>();

        // add 5 ants (nothing should be removed)
        for (int i = 0; i < 5; i++) {
            Ant ant = createAnt(Math.pow(i + 1, 2) % 10 + 2);
            ants.add(ant);
            assertSame(ant, population.addAnt(ant).orElse(null));
            assertEquals(i + 1, population.getSize());
            assertFalse(population.removeAnt().isPresent(), "Failed in iteration " + i);
        }

        // check that ant is not added if the score is too low
        Ant worstAnt = createAnt(1);
        assertFalse(population.addAnt(worstAnt).isPresent());
        assertFalse(population.removeAnt().isPresent());

        ants.sort(Comparator.comparingDouble(Ant::getFitness));

        // add additional 5 ants which are better than the best 5 (every step the worst ant should be removed)
        for (int i = 0; i < 5; i++) {
            Ant ant = createAnt(12 + i);
            assertSame(ant, population.addAnt(ant).orElse(null));
            Ant removed = population.removeAnt().get();
            assertSame(removed, ants.remove(0));
            assertEquals(5, population.getSize());
        }

        assertTrue(ants.isEmpty());
    }

}
