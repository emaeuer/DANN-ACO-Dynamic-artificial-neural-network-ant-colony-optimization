package de.emaeuer.ann;

public interface NeuralNetworkModifier {

    NeuralNetworkModifier splitConnection(NeuronID start, NeuronID end);

    NeuralNetworkModifier addConnection(NeuronID startID, NeuronID endID, double weight);

    NeuralNetworkModifier removeConnection(NeuronID startID, NeuronID endID);

    NeuralNetworkModifier addNeuron(int layerID, double bias);

    NeuralNetworkModifier removeNeuron(NeuronID neuron);

    NeuralNetworkModifier setWeightOfConnection(NeuronID startID, NeuronID endID, double weight);

    NeuralNetworkModifier setBiasOfNeuron(NeuronID neuronID, double bias);

    NeuronID getLastModifiedNeuron();
}

