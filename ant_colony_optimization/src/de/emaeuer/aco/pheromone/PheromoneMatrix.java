package de.emaeuer.aco.pheromone;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.Neuron.NeuronID;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

public class PheromoneMatrix {

    private final PheromoneMatrixModifier modifier = new PheromoneMatrixModifier(this);

    private final List<PheromoneMatrixLayer> pheromoneLayers = new ArrayList<>();

    private PheromoneMatrix() {
    }

    public static PheromoneMatrix buildForNeuralNetwork(NeuralNetwork network) {
        PheromoneMatrix pheromone = new PheromoneMatrix();

        network.stream()
                .map(PheromoneMatrixLayer::buildForNeuralNetworkLayer)
                .forEach(pheromone.pheromoneLayers::add);

        return pheromone;
    }

    public RealVector getWeightPheromoneOfNeuron(NeuronID neuron) {
        return pheromoneLayers.get(neuron.layerID()).getWeightPheromoneOfNeuron(neuron.neuronID());
    }

    public double getBiasPheromoneOfNeuron(NeuronID neuron) {
        return pheromoneLayers.get(neuron.layerID()).getBiasPheromoneOfNeuron(neuron.neuronID());
    }

    public void updatePheromone() {
    }

    public int getNumberOfLayers() {
        return this.pheromoneLayers.size();
    }

    public PheromoneMatrixLayer getLayer(int layerID) {
        return this.pheromoneLayers.get(layerID);
    }
}
