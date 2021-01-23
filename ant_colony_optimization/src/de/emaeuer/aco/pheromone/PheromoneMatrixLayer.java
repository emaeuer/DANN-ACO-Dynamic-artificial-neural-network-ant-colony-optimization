package de.emaeuer.aco.pheromone;

import de.emaeuer.ann.Connection;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.ann.Neuron;
import de.emaeuer.ann.Neuron.NeuronID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PheromoneMatrixLayer {

    private static final double INITIAL_PHEROMONE_VALUE = 0.1;
    private static final double DISSIPATION_FACTOR = 0.1;

    private final List<Neuron> targetNeurons = new ArrayList<>();
    private final List<Neuron> containedNeurons = new ArrayList<>();

    private RealMatrix weightPheromone;

    private RealVector biasPheromone;

    public static PheromoneMatrixLayer buildForNeuralNetworkLayer(NeuralNetworkLayer layer) {
        PheromoneMatrixLayer pheromone = new PheromoneMatrixLayer();

        pheromone.containedNeurons.addAll(layer.getNeurons());

        initializeBiasPheromone(pheromone);
        initializeWeightPheromone(pheromone);

        return pheromone;
    }

    private static void initializeBiasPheromone(PheromoneMatrixLayer pheromone) {
        // set all bias pheromone values to the initial pheromone value
        pheromone.biasPheromone = new ArrayRealVector(pheromone.containedNeurons.size())
                .map(v -> INITIAL_PHEROMONE_VALUE);
    }

    private static void initializeWeightPheromone(PheromoneMatrixLayer pheromone) {
        // find all target neurons of this layer and the corresponding start neurons
        Map<Neuron, List<Neuron>> connections = pheromone.containedNeurons.stream()
                .flatMap(n -> n.getOutgoingConnections().stream())
                .collect(Collectors.groupingBy(Connection::end, Collectors.mapping(Connection::start, Collectors.toList())));
        pheromone.targetNeurons.addAll(connections.keySet());

        if (pheromone.targetNeurons.isEmpty()) {
            pheromone.weightPheromone = null;
            return;
        }

        // initialize weight pheromone matrix
        pheromone.weightPheromone = MatrixUtils.createRealMatrix(pheromone.containedNeurons.size(), pheromone.targetNeurons.size());

        // set pheromone value of all existing connections to the initial pheromone value
        for (Entry<Neuron, List<Neuron>> incomingConnections : connections.entrySet()) {
            int endID = pheromone.targetNeurons.indexOf(incomingConnections.getKey());
            for (Neuron start : incomingConnections.getValue()) {
                pheromone.weightPheromone.setEntry(start.getNeuronInLayerID(), endID, INITIAL_PHEROMONE_VALUE);
            }
        }
    }

    public void updatePheromone() {
        updateSolution();
        dissipatePheromone();
    }

    private void updateSolution() {

    }

    private void dissipatePheromone() {
        if (this.weightPheromone != null) {
            this.weightPheromone = this.weightPheromone.scalarMultiply(DISSIPATION_FACTOR);
        }
    }

    public RealVector getWeightPheromoneOfNeuron(int neuronID) {
        return this.weightPheromone == null ? null : this.weightPheromone.getRowVector(neuronID);
    }

    public double getBiasPheromoneOfNeuron(int neuronID) {
        return this.biasPheromone.getEntry(neuronID);
    }

    public List<Neuron> getContainedNeurons() {
        return this.containedNeurons;
    }

    public List<Neuron> getTargetNeurons() {
        return this.targetNeurons;
    }

    public int indexOfTarget(NeuronID neuron) {
        return IntStream.range(0, this.targetNeurons.size())
                .filter(i -> this.targetNeurons.get(i).getNeuronID().equals(neuron))
                .findFirst()
                .orElse(-1);
    }
}
