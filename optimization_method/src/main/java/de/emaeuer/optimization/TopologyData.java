package de.emaeuer.optimization;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.util.NeuralNetworkUtil;

import java.util.Objects;

public final class TopologyData {

    private final NeuralNetwork instance;
    private String topologyKey;
    private int topologyGroupID;

    public TopologyData(NeuralNetwork instance, int topologyGroupID) {
        this.instance = instance;
        this.topologyKey = NeuralNetworkUtil.getTopologySummary(instance);
        this.topologyGroupID = topologyGroupID;
    }

    public TopologyData copy() {
        TopologyData copy = new TopologyData(this.instance.copy(), this.topologyGroupID);
        copy.topologyKey = this.topologyKey;
        return copy;
    }

    public NeuralNetwork getInstance() {
        return instance;
    }

    public String getTopologyKey() {
        return topologyKey;
    }

    public String refreshTopologyKey() {
        this.topologyKey = NeuralNetworkUtil.getTopologySummary(instance);
        return topologyKey;
    }
    public int getTopologyGroupID() {
        return topologyGroupID;
    }

    public void setTopologyGroupID(int topologyGroupID) {
        this.topologyGroupID = topologyGroupID;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TopologyData) obj;
        return Objects.equals(this.instance, that.instance) &&
                Objects.equals(this.topologyKey, that.topologyKey) &&
                this.topologyGroupID == that.topologyGroupID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instance, topologyKey, topologyGroupID);
    }

    @Override
    public String toString() {
        return "TopologyData[" +
                "instance=" + instance + ", " +
                "topologyKey=" + topologyKey + ", " +
                "topologyGroupID=" + topologyGroupID + ']';
    }


}
