package de.uni.optimization.ga;

import de.uni.ann.NeuralNetwork;
import de.uni.ann.NeuralNetworkLayer;
import de.uni.ann.Neuron;
import de.uni.optimization.Solution;
import org.apache.commons.math3.linear.RealVector;

import java.util.Random;

public class Brain extends Solution {

    private static final Random GENERATOR = new Random();

    private static final double MUTATION_RATE = 0.05;

    private final NeuralNetwork nn;

    public Brain(NeuralNetwork network) {
        this.nn = network.copy();
        this.nn.randomize();
    }

    public Brain(Brain parent1, Brain parent2) {
        this.nn = Brain.crossOver(parent1, parent2);
        mutate();
    }

    private static NeuralNetwork crossOver(Brain parent1, Brain parent2) {
        NeuralNetwork nn = parent1.nn.copy();

        // Take 50 % of neurons of first and other 50% of second parent
        nn.stream()
                .forEach(layer -> {
                    for (Neuron neuron : layer) {
                        if (GENERATOR.nextDouble() > 0.5) {
                            Neuron inheritedNeuron = parent2.nn.getNeuron(neuron.getIdentifier());
                            if (!layer.isInputLayer()) {
                                neuron.setBias(inheritedNeuron.getBias());
                            }
                            if (!layer.isOutputLayer()) {
                                neuron.setOutgoingWeights(inheritedNeuron.getOutgoingWeights());
                            }
                        }
                    }
                });

        return nn;
    }

    private void mutate() {
        nn.stream()
                .filter(layer -> !layer.isInputLayer()) // skip input layer because it contains nothing to mutate
                .flatMap(NeuralNetworkLayer::stream)
                .peek(neuron -> neuron.setBias(mutateValue(neuron.getBias())))
                .flatMap(Neuron::stream)
                .forEach(connection -> connection.setWeight(mutateValue(connection.getWeight())));
    }

    private double mutateValue(double value) {
        if (GENERATOR.nextDouble() < MUTATION_RATE) {
            value = GENERATOR.nextDouble() * 2 - 1;
        }
        return value;
    }

    @Override
    public RealVector process(RealVector input) {
        return this.nn.process(input);
    }
}
