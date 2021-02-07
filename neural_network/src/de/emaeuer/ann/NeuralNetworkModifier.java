package de.emaeuer.ann;

import de.emaeuer.ann.impl.NeuralNetworkModifierImpl;

public interface NeuralNetworkModifier {

    NeuralNetworkModifier splitConnection(NeuronID start, NeuronID end);

    NeuralNetworkModifier addConnection(NeuronID startID, NeuronID endID, double weight);

    NeuralNetworkModifier removeConnection(NeuronID startID, NeuronID endID);

    NeuralNetworkModifier addNeuron(int layerID, double bias);

    NeuralNetworkModifier removeNeuron(NeuronID neuron);

    NeuronID getLastModifiedNeuron();
}

