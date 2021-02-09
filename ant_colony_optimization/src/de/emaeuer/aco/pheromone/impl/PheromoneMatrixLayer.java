package de.emaeuer.aco.pheromone.impl;

import de.emaeuer.aco.configuration.AcoConfiguration;
import de.emaeuer.aco.configuration.AcoConfigurationKeys;
import de.emaeuer.aco.configuration.AcoParameter;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.util.MathUtil;
import org.apache.commons.math3.linear.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;

import static de.emaeuer.aco.configuration.AcoConfigurationKeys.*;
import static de.emaeuer.aco.configuration.AcoParameterNames.NUMBER_OF_DECISIONS;
import static de.emaeuer.aco.configuration.AcoParameterNames.PHEROMONE;
import static de.emaeuer.aco.pheromone.PheromoneMatrix.*;

public class PheromoneMatrixLayer {

    private int layerIndex;

    private final List<NeuronID> targetNeurons = new ArrayList<>();
    private final List<NeuronID> containedNeurons = new ArrayList<>();

    private RealMatrix weightPheromone;
    private RealVector biasPheromone;

    private AcoConfiguration configuration;

    /*
     ##########################################################
     ################# Methods for building ###################
     ##########################################################
    */

    public static PheromoneMatrixLayer buildLayerWithSingleNeuron(NeuronID neuron, AcoConfiguration configuration) {
        PheromoneMatrixLayer pheromone = new PheromoneMatrixLayer();

        pheromone.configuration = configuration;
        pheromone.layerIndex = neuron.getLayerIndex();
        pheromone.containedNeurons.add(neuron);
        pheromone.weightPheromone = null;

        initializeBiasPheromone(pheromone);

        return pheromone;
    }

    public static PheromoneMatrixLayer buildForNeuralNetworkLayer(NeuralNetwork network, int layerIndex, AcoConfiguration configuration) {
        PheromoneMatrixLayer pheromone = new PheromoneMatrixLayer();

        pheromone.configuration = configuration;
        pheromone.layerIndex = layerIndex;
        pheromone.containedNeurons.addAll(network.getNeuronsOfLayer(layerIndex));

        initializeBiasPheromone(pheromone);
        initializeWeightPheromone(pheromone, network, layerIndex);

        return pheromone;
    }

    private static void initializeBiasPheromone(PheromoneMatrixLayer pheromone) {
        double initialPheromoneValue = pheromone.configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE);

        // set all bias pheromone values to the initial pheromone value
        pheromone.biasPheromone = new ArrayRealVector(pheromone.containedNeurons.size())
                .map(v -> initialPheromoneValue);
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
        double initialPheromoneValue = pheromone.configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE);
        for (Entry<NeuronID, List<NeuronID>> outgoingConnections : connections.entrySet()) {
            int endIndex = pheromone.targetNeurons.indexOf(outgoingConnections.getKey());
            for (NeuronID start : outgoingConnections.getValue()) {
                pheromone.weightPheromone.setEntry(start.getNeuronIndex(), endIndex, initialPheromoneValue);
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

        AcoParameter parameter = new AcoParameter();
        double old = this.weightPheromone.getEntry(start.getNeuronIndex(), targetIndex);
        parameter.setParameterValue(PHEROMONE, this.weightPheromone.getEntry(start.getNeuronIndex(), targetIndex));
        parameter.setParameterValue(NUMBER_OF_DECISIONS, getNumberOfTargets(start));
        double pheromoneValue = this.configuration.getValue(ACO_PHEROMONE_UPDATE_FUNCTION, parameter);

        System.out.printf("%s to %s, %f to %f%n", start, target, old, pheromoneValue);

        this.weightPheromone.setEntry(start.getNeuronIndex(), targetIndex, pheromoneValue);
    }

    private double getNumberOfTargets(NeuronID start) {
        return Arrays.stream(this.weightPheromone.getRow(start.getNeuronIndex()))
                .filter(p -> p != 0)
                .count();
    }


    public void updateBiasPheromone(NeuronID neuron) {
        AcoParameter parameter = new AcoParameter();
        parameter.setParameterValue(PHEROMONE, this.biasPheromone.getEntry(neuron.getNeuronIndex()));
        parameter.setParameterValue(NUMBER_OF_DECISIONS, 1); // only one decision possible
        double pheromoneValue = this.configuration.getValue(ACO_PHEROMONE_UPDATE_FUNCTION, parameter);

        this.biasPheromone.setEntry(neuron.getNeuronIndex(), pheromoneValue);
    }

    public void dissipatePheromone() {
        AcoParameter parameter = new AcoParameter();

        DoubleFunction<Double> dissipation = p -> {
            parameter.setParameterValue(PHEROMONE, p);
            return this.configuration.getValue(ACO_PHEROMONE_DISSIPATION_FUNCTION, parameter);
        };

        if (this.weightPheromone != null) {
            MathUtil.modifyMatrix(this.weightPheromone, dissipation);
        }
        this.biasPheromone.mapToSelf(dissipation::apply);
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
        double initialPheromoneValue = this.configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE);

        this.containedNeurons.add(neuron);
        this.weightPheromone = MathUtil.addRowToMatrix(this.weightPheromone);
        this.biasPheromone = MathUtil.addElementToVector(this.biasPheromone);
        this.biasPheromone.setEntry(neuron.getNeuronIndex(), initialPheromoneValue);
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

        double initialPheromoneValue = this.configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE);

        this.weightPheromone.setEntry(start.getNeuronIndex(), endIndex, initialPheromoneValue);
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
            this.weightPheromone = this.targetNeurons.size() != 1
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
