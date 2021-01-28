package de.emaeuer.ann;

import java.util.Objects;

public final class NeuronID {

    private int layerIndex;
    private int neuronIndex;

    public NeuronID(int layerIndex, int neuronIndex) {
        this.layerIndex = layerIndex;
        this.neuronIndex = neuronIndex;
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public void setLayerIndex(int layerIndex) {
        this.layerIndex = layerIndex;
    }

    public int getNeuronIndex() {
        return neuronIndex;
    }

    public void setNeuronIndex(int neuronIndex) {
        this.neuronIndex = neuronIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NeuronID) obj;
        return this.layerIndex == that.layerIndex &&
                this.neuronIndex == that.neuronIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerIndex, neuronIndex);
    }

    @Override
    public String toString() {
        return "NeuronID[" +
                "layerID=" + layerIndex + ", " +
                "neuronID=" + neuronIndex + ']';
    }

}
