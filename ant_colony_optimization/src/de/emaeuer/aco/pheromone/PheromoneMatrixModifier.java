package de.emaeuer.aco.pheromone;

import de.emaeuer.ann.NeuronID;

public interface PheromoneMatrixModifier {

    PheromoneMatrixModifier splitConnection(NeuronID start, NeuronID end, NeuronID intermediate);

    PheromoneMatrixModifier addConnection(NeuronID start, NeuronID end);

    PheromoneMatrixModifier removeConnection(NeuronID start, NeuronID end);

    PheromoneMatrixModifier addNeuron(NeuronID neuron);

    PheromoneMatrixModifier removeNeuron(NeuronID neuron);
}
