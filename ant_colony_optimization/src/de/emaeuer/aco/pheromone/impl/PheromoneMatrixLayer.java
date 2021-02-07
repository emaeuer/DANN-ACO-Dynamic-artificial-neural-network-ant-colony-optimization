package de.emaeuer.aco.pheromone.impl;

import de.emaeuer.aco.Decision;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.util.MathUtil;
import org.apache.commons.math3.linear.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import static de.emaeuer.aco.pheromone.PheromoneMatrix.*;

public class PheromoneMatrixLayer {

    private int layerIndex;

    private final List<NeuronID> targetNeurons = new ArrayList<>();
    private final List<NeuronID> containedNeurons = new ArrayList<>();

    private RealMatrix weightPheromone;
    private RealVector biasPheromone;

    /*
     ##########################################################
     ################# Methods for building ###################
     ##########################################################
    */

    public static PheromoneMatrixLayer buildLayerWithSingleNeuron(NeuronID neuron) {
        PheromoneMatrixLayer pheromone = new PheromoneMatrixLayer();

        pheromone.layerIndex = neuron.getLayerIndex();
        pheromone.containedNeurons.add(neuron);
        pheromone.weightPheromone = null;

        initializeBiasPheromone(pheromone);

        return pheromone;
    }

    public static PheromoneMatrixLayer buildForNeuralNetworkLayer(NeuralNetwork network, int layerIndex) {
        PheromoneMatrixLayer pheromone = new PheromoneMatrixLayer();

        pheromone.layerIndex = layerIndex;
        pheromone.containedNeurons.addAll(network.getNeuronsOfLayer(layerIndex));

        initializeBiasPheromone(pheromone);
        initializeWeightPheromone(pheromone, network, layerIndex);

        return pheromone;
    }

    private static void initializeBiasPheromone(PheromoneMatrixLayer pheromone) {
        // set all bias pheromone values to the initial pheromone value
        pheromone.biasPheromone = new ArrayRealVector(pheromone.containedNeurons.size())
                .map(v -> INITIAL_PHEROMONE_VALUE);
    }

    private static void initializeWeightPheromone(PheromoneMatrixLayer pheromone, NeuralNetwork network, int layerIndex) {
        // find all target neurons of this layer and the corresponding start neurons
        Map<NeuronID, List<NeuronID>> connections = new HashMap<>();
        for (NeuronID neuron : network.getNeuronsOfLayer(layerIndex)) {
            network.getOutgoingConnectionsOfNeuron(neuron)
                    .stream()
                    .peek(n -> connections.putIfAbsent(n, new ArrayList<>()))
                    .forEach(n -> connections.get(n).add(neuron));
        }

        pheromone.targetNeurons.addAll(connections.keySet());

        if (pheromone.targetNeurons.isEmpty()) {
            pheromone.weightPheromone = null;
            return;
        }

        // initialize weight pheromone matrix
        pheromone.weightPheromone = MatrixUtils.createRealMatrix(pheromone.containedNeurons.size(), pheromone.targetNeurons.size());

        // set pheromone value of all existing connections to the initial pheromone value
        for (Entry<NeuronID, List<NeuronID>> outgoingConnections : connections.entrySet()) {
            int endIndex = pheromone.targetNeurons.indexOf(outgoingConnections.getKey());
            for (NeuronID start : outgoingConnections.getValue()) {
                pheromone.weightPheromone.setEntry(start.getNeuronIndex(), endIndex, INITIAL_PHEROMONE_VALUE);
            }
        }
    }

    /*
     ##########################################################
     ################# Methods for accessing ##################
     ##########################################################
    */

    public void updateWeightPheromone(NeuronID start, NeuronID target) {
        int targetIndex = this.targetNeurons.indexOf(target);

        double oldPheromoneValue = this.weightPheromone.getEntry(start.getNeuronIndex(), targetIndex);
        this.weightPheromone.setEntry(start.getNeuronIndex(), targetIndex, PHEROMONE_UPDATE.apply(oldPheromoneValue));
    }

    public void updateBiasPheromone(NeuronID neuron) {
        double oldPheromoneValue = this.biasPheromone.getEntry(neuron.getNeuronIndex());
        this.biasPheromone.setEntry(neuron.getNeuronIndex(), PHEROMONE_UPDATE.apply(oldPheromoneValue));
    }

    public void dissipatePheromone() {
        if (this.weightPheromone != null) {
            MathUtil.modifyMatrix(this.weightPheromone, PHEROMONE_DISSIPATION);
        }
        this.biasPheromone.mapToSelf(PHEROMONE_DISSIPATION::apply);
    }

    public RealVector getWeightPheromoneOfNeuron(int neuronID) {
        return this.weightPheromone == null ? null : this.weightPheromone.getRowVector(neuronID);
    }

    public double getBiasPheromoneOfNeuron(int neuronID) {
        return this.biasPheromone.getEntry(neuronID);
    }

    public List<NeuronID> getContainedNeurons() {
        return this.containedNeurons;
    }

    public List<NeuronID> getTargetNeurons() {
        return this.targetNeurons;
    }

    public int indexOfTarget(NeuronID neuron) {
        return IntStream.range(0, this.targetNeurons.size())
                .filter(i -> this.targetNeurons.get(i).equals(neuron))
                .findFirst()
                .orElse(-1);
    }

    /*
     ##########################################################
     ################# Methods for modifying ##################
     ##########################################################
    */

    public void addNeuron(NeuronID neuron) {
        this.containedNeurons.add(neuron);
        this.weightPheromone = MathUtil.addRowToMatrix(this.weightPheromone);
        this.biasPheromone = MathUtil.addElementToVector(this.biasPheromone);
        this.biasPheromone.setEntry(neuron.getNeuronIndex(), INITIAL_PHEROMONE_VALUE);
    }

    /**
     * Tells this layer that this neuron is removed from neural network.
     * Removes it from the layer which contains it, remove it from targets or do nothing if it has no association with
     * this layer
     *
     * @param neuron to remove
     */
    public void removeNeuron(NeuronID neuron) {
        // remove the neuron if it is contained in this layer
        if (neuron.getLayerIndex() == this.layerIndex) {
            // shifting other neurons should not be necessary because the objects should already have been updated
            // by the neural network
            this.weightPheromone = MathUtil.removeRowFromMatrix(this.weightPheromone, neuron.getNeuronIndex());
            // if the neuron wasn't the last one in this layer there are two equal objects (because the neurons were already shifted in the neural network)
            // remove the first one because it is the removed one
            this.containedNeurons.remove(neuron);
            // through removing this neuron there may be targets without a connection to this layer
            removeUnusedTargetNeurons();
        }

        // remove it from target if there is one connection from this layer to the neuron
        // ATTENTION: because target neurons has no particular order there may be two equal neurons because on is the neuron to remove and the other one was
        // updated in neural network to have the same id by shifting it
        // solution is to find the targetIndex with the specific reference to the removed neuron --> necessary to pass the object which was removed in the
        // neural network and not just an equal object
        OptionalInt targetIndex = IntStream.range(0, this.targetNeurons.size())
                .filter(i -> this.targetNeurons.get(i) == neuron)
                .findFirst();
        if (targetIndex.isPresent()) {
            this.weightPheromone = this.targetNeurons.size() != 1
                    ? MathUtil.removeColumnFromMatrix(this.weightPheromone, targetIndex.getAsInt())
                    : null;
            this.targetNeurons.remove(neuron);
        }
    }

    public void addConnection(NeuronID start, NeuronID end) {
        if (start.getLayerIndex() != this.layerIndex) {
            throw new IllegalArgumentException(String.format("Can't add outgoing of neuron %s to layer %d", start, this.layerIndex));
        }

        // find index of target neuron and eventually add new target neuron
        int endIndex = this.targetNeurons.contains(end)
                ? this.targetNeurons.indexOf(end)
                : addNewTargetNeuron(end);

        this.weightPheromone.setEntry(start.getNeuronIndex(), endIndex, INITIAL_PHEROMONE_VALUE);
    }

    public void removeConnection(NeuronID start, NeuronID end) {
        int endIndex = this.targetNeurons.indexOf(end);

        if (endIndex == -1) {
            throw new IllegalArgumentException(String.format("Can't remove non existing connection from %s to %s from layer %d", start, end, this.layerIndex));
        }

        this.weightPheromone.setEntry(start.getNeuronIndex(), endIndex, 0);
        removeTargetNeuronIfUnused(endIndex);
    }

    private void removeUnusedTargetNeurons() {
        // start at the end because it makes removing easier (because of shifted indices)
        for (int i = this.weightPheromone.getColumnDimension() - 1; i >= 0; i--) {
            removeTargetNeuronIfUnused(i);
        }
    }

    private void removeTargetNeuronIfUnused(int targetIndex) {
        // check if column sum is 0 --> no connection from this layer to target neuron
        if (this.weightPheromone.getColumnVector(targetIndex).getL1Norm() == 0) {
            this.weightPheromone = this.weightPheromone = this.targetNeurons.size() != 1
                    ? MathUtil.removeColumnFromMatrix(this.weightPheromone, targetIndex)
                    : null;
            this.targetNeurons.remove(targetIndex);
        }
    }

    private int addNewTargetNeuron(NeuronID target) {
        this.targetNeurons.add(target);

        // if this layer had no connections previously create new matrix or add new column to existing matrix
        this.weightPheromone = this.weightPheromone == null
                ? MatrixUtils.createRealMatrix(1, 1)
                : MathUtil.addColumnToMatrix(this.weightPheromone);

        return this.targetNeurons.size() - 1;
    }

    public int getLayerIndex() {
        return this.layerIndex;
    }

    public void setLayerIndex(int layerIndex) {
        this.layerIndex = layerIndex;
    }

}
