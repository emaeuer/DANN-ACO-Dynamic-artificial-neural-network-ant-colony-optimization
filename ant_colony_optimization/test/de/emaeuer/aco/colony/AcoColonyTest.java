package de.emaeuer.aco.colony;

import de.emaeuer.aco.Ant;
import de.emaeuer.aco.Decision;
import de.emaeuer.aco.configuration.AcoConfiguration;
import de.emaeuer.aco.configuration.AcoParameter;
import de.emaeuer.aco.pheromone.PheromoneMatrix;
import de.emaeuer.aco.pheromone.impl.PheromoneMatrixLayer;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkBuilder;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.optimization.configuration.ConfigurationValue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.emaeuer.aco.configuration.AcoConfigurationKeys.*;
import static de.emaeuer.aco.configuration.AcoParameterNames.NUMBER_OF_DECISIONS;
import static de.emaeuer.aco.configuration.AcoParameterNames.PHEROMONE;
import static org.junit.jupiter.api.Assertions.*;

public class AcoColonyTest {

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

    private AcoConfiguration buildConfiguration(int colonySize) {
        return new AcoConfiguration(Arrays.asList(
                new ConfigurationValue<>(ACO_COLONY_SIZE, Integer.toString(colonySize)),
                new ConfigurationValue<>(ACO_PHEROMONE_UPDATE_FUNCTION, "tanh(p/2)/tanh(1)", PHEROMONE)
        ));
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testBasicSolutionCreation() {
        // create colony and generate solutions
        AcoConfiguration configuration = buildConfiguration(5);
        NeuralNetwork nn = buildNeuralNetwork(1, 1);
        AcoColony colony = new AcoColony(nn, configuration);

        List<Ant> solutions = colony.nextIteration();

        // general checks
        assertEquals(5, solutions.size());

        // check generated solution
        for (Ant solution : solutions) {
            List<Decision> decisions = solution.getSolution();
            assertEquals(2, decisions.size());

            assertEquals(new Decision(new NeuronID(0, 0), 0, 0), decisions.get(0));
            assertEquals(new NeuronID(1, 0), decisions.get(1).neuronID());

            // check neural network of ant was modified according to solution
            NeuralNetwork antNN = solution.getBrain();
            assertEquals(decisions.get(1).biasValue(), antNN.getBiasOfNeuron(new NeuronID(1, 0)));
            assertEquals(decisions.get(1).weightValue(), antNN.getWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0)));
        }
    }

    @Test
    public void testComplexSolutionCreation() {
        // create colony and generate solutions
        AcoConfiguration configuration = buildConfiguration(1);
        NeuralNetwork nn = buildNeuralNetwork(3, 5, 5, 4);
        AcoColony colony = new AcoColony(nn, configuration);

        List<Ant> solutions = colony.nextIteration();

        // general checks
        assertEquals(1, solutions.size());

        // check generated solution
        Ant solution = solutions.get(0);
        List<Decision> decisions = solution.getSolution();
        assertEquals(4, decisions.size());

        // check neural network of ant was modified according to solution
        NeuralNetwork antNN = solution.getBrain();
        NeuronID current = decisions.get(0).neuronID();
        assertEquals(0, current.getLayerIndex());

        for (Decision decision : decisions.subList(1, decisions.size())) {
            assertEquals(decision.biasValue(), antNN.getBiasOfNeuron(decision.neuronID()));
            assertEquals(decision.weightValue(), antNN.getWeightOfConnection(current, decision.neuronID()));
            current = decision.neuronID();
        }
    }

    @Test
    public void testBasicSolutionUpdate() {
        // create colony, generate solutions and update best solution
        AcoConfiguration configuration = buildConfiguration(5);
        NeuralNetwork nn = buildNeuralNetwork(1, 1);
        AcoColony colony = new AcoColony(nn, configuration);

        List<Ant> solutions = colony.nextIteration();
        Ant bestSolution = solutions.get(0);
        bestSolution.setFitness(10); // first ant updates
        colony.updateSolutions();

        // check neural network of ant colony was updated
        List<Decision> decisions = bestSolution.getSolution();
        assertEquals(decisions.get(1).biasValue(), nn.getBiasOfNeuron(new NeuronID(1, 0)));
        assertEquals(decisions.get(1).weightValue(), nn.getWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0)));

        // check pheromone matrix was updated
        PheromoneMatrix matrix = colony.getPheromoneMatrix();
        AcoParameter parameter = new AcoParameter();
        parameter.setParameterValue(PHEROMONE, configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE));
        parameter.setParameterValue(PHEROMONE, configuration.getValue(ACO_PHEROMONE_UPDATE_FUNCTION, parameter));
        double expectedPheromone = configuration.getValue(ACO_PHEROMONE_DISSIPATION_FUNCTION, parameter);

        assertEquals(expectedPheromone, matrix.getStartPheromoneValues().getEntry(0));
        assertEquals(expectedPheromone, matrix.getBiasPheromoneOfNeuron(new NeuronID(1, 0)));
        assertEquals(expectedPheromone, matrix.getWeightPheromoneOfNeuron(new NeuronID(0, 0)).getEntry(0));
    }

    @Test
    public void testComplexSolutionUpdate() {
        // create colony and generate solutions
        AcoConfiguration configuration = buildConfiguration(5);
        NeuralNetwork nn = buildNeuralNetwork(3, 5, 5, 4);
        AcoColony colony = new AcoColony(nn, configuration);

        List<Ant> solutions = colony.nextIteration();
        Ant bestSolution = solutions.get(0);
        bestSolution.setFitness(10); // first ant updates
        colony.updateSolutions();

        // check neural network of colony was modified according to solution
        List<Decision> decisions = bestSolution.getSolution();
        NeuronID current = decisions.get(0).neuronID();
        assertEquals(0, current.getLayerIndex());

        for (Decision decision : decisions.subList(1, decisions.size())) {
            // check only bias of selected neuron was changed
            for (NeuronID neuron : nn.getNeuronsOfLayer(decision.neuronID().getLayerIndex())) {
                if (neuron.equals(decision.neuronID())) {
                    assertEquals(decision.biasValue(), nn.getBiasOfNeuron(decision.neuronID()));
                } else {
                    assertEquals(0, nn.getBiasOfNeuron(neuron));
                }
            }

            // check only weight of selected connection was changed
            for (NeuronID neuron : nn.getNeuronsOfLayer(current.getLayerIndex())) {
                for (NeuronID target : nn.getOutgoingConnectionsOfNeuron(neuron)) {
                    if (neuron.equals(current) && target.equals(decision.neuronID())) {
                        assertEquals(decision.weightValue(), nn.getWeightOfConnection(current, decision.neuronID()));
                    } else {
                        assertEquals(0, nn.getWeightOfConnection(neuron, target));
                    }
                }
            }

            current = decision.neuronID();
        }

        // check pheromone matrix was updated
        PheromoneMatrix matrix = colony.getPheromoneMatrix();

        AcoParameter parameter = new AcoParameter();
        parameter.setParameterValue(PHEROMONE, configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE));
        parameter.setParameterValue(PHEROMONE, configuration.getValue(ACO_PHEROMONE_UPDATE_FUNCTION, parameter));
        double updatedPheromone = configuration.getValue(ACO_PHEROMONE_DISSIPATION_FUNCTION, parameter);
        parameter.setParameterValue(PHEROMONE, configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE));
        double dissipatedPheromone = configuration.getValue(ACO_PHEROMONE_DISSIPATION_FUNCTION, parameter);

        current = decisions.get(0).neuronID();
        for (Decision decision : decisions.subList(1, decisions.size())) {
            PheromoneMatrixLayer currentLayer = matrix.getLayer(current.getLayerIndex());
            PheromoneMatrixLayer nextLayer = matrix.getLayer(decision.neuronID().getLayerIndex());

            // check only bias of selected neuron was changed
            for (NeuronID containedNeuron : nextLayer.getContainedNeurons()) {
                if (containedNeuron.equals(decision.neuronID())) {
                    assertEquals(updatedPheromone, matrix.getBiasPheromoneOfNeuron(containedNeuron));
                } else {
                    assertEquals(dissipatedPheromone, matrix.getBiasPheromoneOfNeuron(containedNeuron));
                }
            }

            // check only weight of selected connection was changed
            for (NeuronID containedNeuron : currentLayer.getContainedNeurons()) {
                for (NeuronID target : nn.getOutgoingConnectionsOfNeuron(containedNeuron)) {
                    if (target.equals(decision.neuronID()) && containedNeuron.equals(current)) {
                        assertEquals(updatedPheromone, matrix.getWeightPheromoneOfNeuron(current).getEntry(matrix.getLayer(current.getLayerIndex()).indexOfTarget(decision.neuronID())));
                    } else {
                        assertEquals(dissipatedPheromone, matrix.getWeightPheromoneOfNeuron(containedNeuron).getEntry(matrix.getLayer(current.getLayerIndex()).indexOfTarget(target)));
                    }
                }
            }

            current = decision.neuronID();
        }
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
