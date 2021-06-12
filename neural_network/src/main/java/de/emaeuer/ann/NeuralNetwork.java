package de.emaeuer.ann;

import de.emaeuer.ann.impl.layer.based.NeuralNetworkBuilderImpl;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;

public interface NeuralNetwork {

    static NeuralNetworkBuilder<? extends NeuralNetworkLayerBuilder> build() {
        return new NeuralNetworkBuilderImpl();
    }

    RealVector process(RealVector input);

    NeuralNetworkModifier modify();

    int getDepth();

    List<NeuronID> getOutgoingConnectionsOfNeuron(NeuronID neuron);

    List<NeuronID> getIncomingConnectionsOfNeuron(NeuronID neuron);

    boolean neuronHasConnectionTo(NeuronID start, NeuronID end);

    boolean neuronHasConnectionToLayer(NeuronID start, int layerIndex);

    double getWeightOfConnection(NeuronID start, NeuronID end);

    void setWeightOfConnection(NeuronID start, NeuronID end, double weight);

    double getBiasOfNeuron(NeuronID newNeuron);

    void setBiasOfNeuron(NeuronID currentNeuron, double biasValue);

    List<NeuronID> getNeuronsOfLayer(int layerIndex);

    NeuralNetwork copy();

    boolean isOutputNeuron(NeuronID currentNeuron);

    boolean isInputNeuron(NeuronID currentNeuron);

    boolean usesExplicitBias();

    double getMaxWeightValue();

    double getMinWeightValue();

}
