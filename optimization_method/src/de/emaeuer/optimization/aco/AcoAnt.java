package de.emaeuer.optimization.aco;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariablesBuilder;
import de.emaeuer.optimization.aco.colony.AcoColony;
import de.emaeuer.optimization.aco.configuration.AcoConfiguration;
import de.emaeuer.optimization.aco.configuration.AcoParameter;
import de.emaeuer.optimization.aco.pheromone.PheromoneMatrix;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.optimization.Solution;
import org.apache.commons.math3.linear.RealVector;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static de.emaeuer.optimization.aco.configuration.AcoConfiguration.*;
import static de.emaeuer.optimization.aco.configuration.AcoParameter.*;

public class AcoAnt implements Solution {

    private final AcoColony colony;

    private final List<Decision> solution = new ArrayList<>();

    private final NeuralNetwork brain;

    private double fitness = -1;

    public AcoAnt(AcoColony colony) {
        this.colony = colony;
        this.brain = colony.getNeuralNetwork().copy();
    }

    public void generateSolution() {
        NeuronID currentNeuron = makeStartDecision();
        solution.add(new Decision(currentNeuron, 0, 0));

        // TODO add probability for not ending iteration and use recurrent connection of output if one exists
        while (!this.brain.isOutputNeuron(currentNeuron)) {
            Entry<Double, NeuronID> connectionDecision = makeConnectionDecision(currentNeuron);
            currentNeuron = connectionDecision.getValue();
            double biasDecision = makeBiasDecision(currentNeuron);

            this.solution.add(new Decision(currentNeuron, connectionDecision.getKey(), biasDecision));
        }
    }

    private NeuronID makeStartDecision() {
        PheromoneMatrix pheromone = this.colony.getPheromoneMatrix();

        int neuronIndex = RandomUtil.selectRandomElementFromVector(pheromone.getStartPheromoneValues(), true);
        return new NeuronID(0, neuronIndex);
    }

    private Entry<Double, NeuronID> makeConnectionDecision(NeuronID neuron) {
        ConfigurationHandler<AcoConfiguration> configuration = this.colony.getConfiguration();
        PheromoneMatrix pheromone = this.colony.getPheromoneMatrix();

        RealVector pheromoneVector = pheromone.getWeightPheromoneOfNeuron(neuron);

        ConfigurationVariablesBuilder<AcoParameter> variables = ConfigurationVariablesBuilder.<AcoParameter>build()
                .with(PHEROMONE, pheromoneVector.getL1Norm() / pheromoneVector.getDimension()) // pheromone value is average pheromone
                .with(NUMBER_OF_DECISIONS, pheromoneVector.getDimension())
                .with(CURRENT_LAYER_INDEX, neuron.getLayerIndex())
                .with(NEURAL_NETWORK_DEPTH, this.brain.getDepth())
                .with(RANDOM_PARAMETER, Math.random());

        // add element to pheromone vector with probability for completely random selection
        // invert the probability
        pheromoneVector = pheromoneVector.append(1 - configuration.getValue(ACO_NEW_CONNECTION_PROBABILITY, Double.class, variables.getVariables()));

        // select a random target
        int targetIndex = RandomUtil.selectRandomElementFromVector(pheromoneVector, true);
        NeuronID target = targetIndex >= pheromoneVector.getDimension() - 1
                ? selectRandomTargetAndModifyBrain(neuron, variables.getVariables())
                : pheromone.getTargetOfNeuronByIndex(neuron, targetIndex);

        // choose new weight value according to current pheromone value
        double currentPheromone;
        if (getBrain().neuronHasConnectionTo(neuron, target)) {
            if (this.colony.getNeuralNetwork().neuronHasConnectionTo(neuron, target)) {
                currentPheromone = pheromone.getWeightPheromoneOfConnection(neuron, target);
            } else {
                return makeConnectionDecision(neuron);
            }
        } else {
            getBrain().modify().addConnection(neuron, target, 0);
            currentPheromone = configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE, Double.class);
        }

        variables.with(PHEROMONE, currentPheromone)
            .with(NUMBER_OF_DECISIONS, 0);

        double weightValue = RandomUtil.getNormalDistributedValue(currentPheromone, configuration.getValue(ACO_STANDARD_DEVIATION_FUNCTION, Double.class, variables.getVariables()));

        // update decision in neural network
        this.brain.setWeightOfConnection(neuron, target, weightValue);

        return new SimpleEntry<>(weightValue, target);
    }

    private NeuronID selectRandomTargetAndModifyBrain(NeuronID currentPosition, Map<String, Double> variables) {
        ConfigurationHandler<AcoConfiguration> configuration = this.colony.getConfiguration();
        int layerIndex = configuration.getValue(ACO_NEW_LAYER_SELECTION_PROBABILITY, Integer.class, variables);

        if (layerIndex >= this.brain.getDepth() || layerIndex < 1) {
            throw new IllegalArgumentException(String.format("Function %s is not an valid inverted probability distribution for the layer selection",
                    configuration.getStringRepresentation(ACO_NEW_LAYER_SELECTION_PROBABILITY)));
        }

        int neuronIndex = RandomUtil.getNextInt(0, getBrain().getNeuronsOfLayer(layerIndex).size());
        return getBrain().getNeuronsOfLayer(layerIndex).get(neuronIndex);
    }

    private double makeBiasDecision(NeuronID currentNeuron) {
        ConfigurationHandler<AcoConfiguration> configuration = this.colony.getConfiguration();
        PheromoneMatrix pheromone = this.colony.getPheromoneMatrix();

        // choose new bias value according to current pheromone value
        double currentPheromone = pheromone.getBiasPheromoneOfNeuron(currentNeuron);

        Map<String, Double> variables = ConfigurationVariablesBuilder.<AcoParameter>build()
                .with(PHEROMONE, currentPheromone)
                .with(NUMBER_OF_DECISIONS, 1)
                .with(CURRENT_LAYER_INDEX, currentNeuron.getLayerIndex())
                .with(NEURAL_NETWORK_DEPTH, this.brain.getDepth())
                .getVariables();

        double biasValue = RandomUtil.getNormalDistributedValue(currentPheromone, configuration.getValue(ACO_STANDARD_DEVIATION_FUNCTION, Double.class, variables));

        // update decision in neural network
        this.brain.setBiasOfNeuron(currentNeuron, biasValue);

        return biasValue;
    }

    @Override
    public RealVector process(RealVector input) {
        return this.brain.process(input);
    }

    @Override
    public double getFitness() {
        return fitness;
    }

    @Override
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public NeuralNetwork getNeuralNetwork() {
        return this.brain;
    }

    public List<Decision> getSolution() {
        return solution;
    }

    public NeuralNetwork getBrain() {
        return brain;
    }

    public int getColonyNumber() {
        return this.colony.getColonyNumber();
    }
}
