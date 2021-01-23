package de.emaeuer.ann;

import de.emaeuer.ann.Neuron.NeuronID;
import de.emaeuer.ann.util.NeuralNetworkLayerBuilder;
import de.emaeuer.ann.util.NeuralNetworkLayerModifier;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;
import java.util.stream.Stream;

public interface NeuralNetworkLayer extends Iterable<Neuron> {

    public enum LayerType {
        INPUT,
        HIDDEN,
        OUTPUT;
    }

    public static NeuralNetworkLayerBuilder build() {
        return new NeuralNetworkLayerBuilder();
    }

    public NeuralNetworkLayerModifier modify();

    public Stream<Neuron> stream();

    public Neuron getNeuron(int neuronID);

    public int getLayerID();

    public RealVector process();

    public RealVector process(RealVector externalInput);

    public boolean isInputLayer();

    public boolean isOutputLayer();

    public int getNumberOfNeurons();

    public RealVector getActivation();

    public double getActivationOf(int inLayerID);

    public RealVector getBias();

    public double getBiasOf(int inLayerID);

    public void setBiasOf(int inLayerID, double bias);

    public RealMatrix getWeights();

    public double getWeightOf(NeuronID start, NeuronID end);

    public void setWeightOf(NeuronID start, NeuronID end, double weight);

    public List<Neuron> getNeurons();

    public LayerType getType();

    public NeuralNetwork getNeuralNetwork();
}
