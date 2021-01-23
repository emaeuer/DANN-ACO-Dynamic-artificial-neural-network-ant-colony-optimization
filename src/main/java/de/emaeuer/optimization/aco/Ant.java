package de.emaeuer.optimization.aco;

import de.emaeuer.ann.Connection;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.Neuron;
import de.emaeuer.math.MathUtil;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.aco.pheromone.ComplexPheromoneValue;
import de.emaeuer.optimization.aco.pheromone.CompositePheromoneMatrix;
import de.emaeuer.optimization.aco.pheromone.LayerPheromoneMatrix;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;

public class Ant extends Solution {

    private int colonyNumber;
    private boolean isColonyBest = false;

    public record Decision(Neuron.NeuronID neuronID, double weightValue, double biasValue) {}

    // fixed seed for reproducibility
    public static final Random GENERATOR = new Random(76564511);

    private final NeuralNetwork nn;
    private final CompositePheromoneMatrix pheromoneValues;

    private final List<Decision> solution = new ArrayList<>();

    public Ant(NeuralNetwork nn, CompositePheromoneMatrix pheromoneValues) {
        this.nn = nn;
        this.pheromoneValues = pheromoneValues;
    }

    @Override
    public RealVector process(RealVector input) {
        return this.nn.process(input);
    }

    public void walk() {
        // choose start neuron evenly distributed
        Neuron.NeuronID startNeuronID = new Neuron.NeuronID(0, GENERATOR.nextInt(this.nn.getInputLayer().getNumberOfNeurons()));
        Neuron currentPosition = this.nn.getNeuron(startNeuronID);
        solution.add(new Decision(startNeuronID, 0, 0));

        for (LayerPheromoneMatrix currentPheromoneMatrix: pheromoneValues) {
            Map.Entry<Double, Neuron> connectionDecision = makeConnectionDecision(currentPheromoneMatrix, currentPosition);
            double biasDecision = makeBiasDecision(currentPheromoneMatrix, connectionDecision.getValue());

            currentPosition = connectionDecision.getValue();
            this.solution.add(new Decision(currentPosition.getIdentifier(), connectionDecision.getKey(), biasDecision));
        }
    }

    private Map.Entry<Double, Neuron> makeConnectionDecision(LayerPheromoneMatrix pheromoneMatrix, Neuron neuron) {
        RealVector weightPheromone = pheromoneMatrix.getWeightDecisionVector(neuron.getIndexInLayer());
        // choose at least fixed connection
        int weightDecision = MathUtil.selectRandomElementFromVector(weightPheromone, true);

        ComplexPheromoneValue selectedPheromone = pheromoneMatrix.getWeightPheromoneValue(weightDecision, neuron.getIndexInLayer());
        NormalDistribution distribution = getNormalDistributionForPheromone(selectedPheromone);

        // update decision in neural network
        Connection connection = neuron.getConnections().get(weightDecision);
        connection.setWeight(distribution.sample()); // choose a random value from normal distribution with inversion method

        // return decision
        return new AbstractMap.SimpleEntry<>(connection.getWeight(), connection.getEnd());
    }

    private double makeBiasDecision(LayerPheromoneMatrix pheromoneMatrix, Neuron neuron) {
        ComplexPheromoneValue selectedPheromone = pheromoneMatrix.getBiasPheromoneValue(neuron.getIndexInLayer());
        NormalDistribution distribution = getNormalDistributionForPheromone(selectedPheromone);

        // update decision in neural network
        neuron.setBias(distribution.sample()); // choose a random value from normal distribution with inversion method

        return neuron.getBias();
    }

    private NormalDistribution getNormalDistributionForPheromone(ComplexPheromoneValue selectedPheromone) {
        double standardDerivation = (1 - 0.5 * selectedPheromone.getFixed()) / (10 * selectedPheromone.getFixed() + 1);
        return new NormalDistribution(selectedPheromone.getValue(), standardDerivation);
    }

    public NeuralNetwork getNeuralNetwork() {
        return this.nn;
    }

    public List<Decision> getSolution() {
        if (solution.isEmpty()) {
            throw new UnsupportedOperationException("Ant hasn't created a solution (call walk-method first)");
        }
        return this.solution;
    }

    public int getColonyNumber() {
        return this.colonyNumber;
    }

    public void setColonyNumber(int colonyNumber) {
        this.colonyNumber = colonyNumber;
    }

    public boolean isColonyBest() {
        return isColonyBest;
    }

    public void setColonyBest(boolean colonyBest) {
        isColonyBest = colonyBest;
    }
}
