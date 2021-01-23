package de.emaeuer.aco.pheromone;

import de.emaeuer.ann.Neuron;
import de.emaeuer.ann.Neuron.NeuronID;

public class PheromoneMatrixModifier {

    private final PheromoneMatrix matrix;

    public PheromoneMatrixModifier(PheromoneMatrix matrix) {
        this.matrix = matrix;
    }

    public PheromoneMatrixModifier addSplitConnection(NeuronID start, NeuronID end) {
        return this;
    }

    public PheromoneMatrixModifier addConnection(NeuronID startID, NeuronID endID) {
        return this;
    }

    public PheromoneMatrixModifier removeConnection(NeuronID startID, NeuronID endID) {
        return this;
    }

    public PheromoneMatrixModifier addNeuron(NeuronID startID, NeuronID endID) {
        return this;
    }

    public PheromoneMatrixModifier removeNeuron(NeuronID startID, NeuronID endID) {
        return this;
    }

}
