package de.uni.ann;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

public class NeuralNetworkModifier {

    public static class NeuralNetworkLayerModifier {
        private final NeuralNetworkLayer layer;
        private final NeuralNetworkModifier nnModifier;

        public NeuralNetworkLayerModifier(NeuralNetworkLayer layer, NeuralNetworkModifier modifier) {
            this.layer = layer;
            this.nnModifier = modifier;
        }

        public NeuralNetworkModifier finish() {
            return this.nnModifier;
        }

        public NeuralNetworkLayerModifier setWeightOfConnection(int startNeuronIndex, int endNeuronIndex, double weight) {
            this.layer.getWeights().setEntry(endNeuronIndex, startNeuronIndex, weight);
            return this;
        }

        public NeuralNetworkLayerModifier setWeightsOfNeuron(int neuronIndex, RealVector weights) {
            if (weights.getDimension() != this.layer.getWeights().getRowDimension()) {
                throw new IllegalArgumentException(String.format(
                        "Expected an vector with a dimension of %d but received one with dimension %d",
                        this.layer.getWeights().getColumnDimension(), weights.getDimension()));
            }
            this.layer.getWeights().setColumnVector(neuronIndex, weights);
            return this;
        }

        public NeuralNetworkLayerModifier setBiasOfNeuron(int neuronIndex, double bias) {
            this.layer.getBias().setEntry(neuronIndex, bias);
            return this;
        }

        public NeuralNetworkLayerModifier setWeightsOfLayer(RealMatrix weights) {
            boolean haveEqualSize = weights.getColumnDimension() == this.layer.getWeights().getColumnDimension();
            haveEqualSize &= weights.getRowDimension() == this.layer.getWeights().getRowDimension();
            if (!haveEqualSize) {
                throw new IllegalArgumentException(String.format("Expected an %d x %d matrix but received a %d x %d matrix",
                        this.layer.getWeights().getColumnDimension(), this.layer.getWeights().getRowDimension(),
                        weights.getColumnDimension(), weights.getRowDimension()));
            }

            for (int i = 0; i < this.layer.getWeights().getColumnDimension(); i++) {
                this.layer.getWeights().setColumnVector(i, weights.getColumnVector(i));
            }

            return this;
        }

        public NeuralNetworkLayerModifier setBiasOfLayer(RealVector bias) {
            if (this.layer.getBias().getDimension() != bias.getDimension()) {
                throw new IllegalArgumentException(String.format("Expected an vector with a dimension of %d but received one with dimension %d",
                        this.layer.getBias().getDimension(), bias.getDimension()));
            }

            for (int i = 0; i < this.layer.getBias().getDimension(); i++) {
                this.layer.getBias().setEntry(i, bias.getEntry(i));
            }

            return this;
        }

    }

    private final NeuralNetwork nn;
    private final List<NeuralNetworkLayerModifier> layerModifiers = new ArrayList<>();
    private int currentLayerIndex = 1;

    public NeuralNetworkModifier(NeuralNetwork nn) {
        this.nn = nn;

        // initialize all layer modifiers
        NeuralNetworkLayer layer = this.nn.getInputLayer();
        while (layer != null) {
            this.layerModifiers.add(new NeuralNetworkLayerModifier(layer, this));
            layer = layer.getNextLayer();
        }
    }

    public NeuralNetworkLayerModifier restartModification() {
        this.currentLayerIndex = 1;
        return this.layerModifiers.get(this.currentLayerIndex);
    }

    public NeuralNetworkLayerModifier modifyLayer(int layerIndex) {
        if (layerIndex >= this.nn.getNumberOfLayers()) {
            throw new IndexOutOfBoundsException(String.format("Can't modify layer number %d because the neural network only consists of %d layers", layerIndex, this.nn.getNumberOfLayers()));
        } else if (layerIndex < 1) {
            throw new IndexOutOfBoundsException("Can't modify input layer or layers with negative index");
        }

        currentLayerIndex = layerIndex;
        return this.layerModifiers.get(this.currentLayerIndex);
    }

    public NeuralNetworkLayerModifier modifyNextLayer() {
        if (currentLayerIndex >= this.nn.getNumberOfLayers() - 1) {
            throw new IndexOutOfBoundsException("Can't retrieve next layer because the modifier already reached the output layer");
        }
        currentLayerIndex++;
        return this.layerModifiers.get(this.currentLayerIndex);
    }

    public NeuralNetworkLayerModifier modifyPreviousLayer() {
        if (currentLayerIndex == 1) {
            throw new IndexOutOfBoundsException("Can't retrieve previous layer because the modifier already reached the first layer");
        }
        currentLayerIndex--;
        return this.layerModifiers.get(this.currentLayerIndex);
    }


}
