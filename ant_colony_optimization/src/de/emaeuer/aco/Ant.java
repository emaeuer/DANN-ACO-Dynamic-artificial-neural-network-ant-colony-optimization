package de.emaeuer.aco;

import de.emaeuer.aco.colony.AcoColony;
import de.emaeuer.aco.pheromone.PheromoneMatrix;
import de.emaeuer.aco.util.RandomUtil;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.optimization.Solution;
import org.apache.commons.math3.linear.RealVector;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.DoubleFunction;

public class Ant implements Solution {

    public static final DoubleFunction<Double> STANDARD_DEVIATION_FUNCTION = d -> (1 - 0.5 * d) / (10 * d + 1);

    private final AcoColony colony;

    private final List<Decision> solution = new ArrayList<>();

    private final NeuralNetwork brain;

    private double fitness = -1;

    public Ant(AcoColony colony) {
        this.colony = colony;
        this.brain = colony.getNeuralNetwork().copy();
    }

    public void generateSolution() {
        NeuronID currentNeuron = makeStartDecision();
        solution.add(new Decision(currentNeuron, 0, 0));

        // TODO add probability for not ending iteration and walk recurrent connection of output if exists
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
        System.out.println("Start " + neuronIndex);
        return new NeuronID(0, neuronIndex);
    }

    private Entry<Double, NeuronID> makeConnectionDecision(NeuronID neuron) {
        PheromoneMatrix pheromone = this.colony.getPheromoneMatrix();

        // select a random target
        // TODO make it possible to select target which currently doesn't exists
        int targetIndex = RandomUtil.selectRandomElementFromVector(pheromone.getWeightPheromoneOfNeuron(neuron), true);
        NeuronID target = pheromone.getTargetOfNeuronByIndex(neuron, targetIndex);

        // choose new weight value according to current pheromone value
        double currentPheromone = pheromone.getWeightPheromoneOfNeuron(neuron).getEntry(targetIndex);
        double weightValue = RandomUtil.getNormalDistributedValue(currentPheromone, STANDARD_DEVIATION_FUNCTION.apply(currentPheromone));

        // update decision in neural network
        this.brain.setWeightOfConnection(neuron, target, weightValue);

        return new SimpleEntry<>(weightValue, target);
    }

    private double makeBiasDecision(NeuronID currentNeuron) {
        PheromoneMatrix pheromone = this.colony.getPheromoneMatrix();

        // choose new bias value according to current pheromone value
        double currentPheromone = pheromone.getBiasPheromoneOfNeuron(currentNeuron);
        double biasValue = RandomUtil.getNormalDistributedValue(currentPheromone, STANDARD_DEVIATION_FUNCTION.apply(currentPheromone));

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
