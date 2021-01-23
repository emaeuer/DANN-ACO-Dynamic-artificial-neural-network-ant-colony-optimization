package de.emaeuer.ann;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Neuron implements Iterable<Connection> {

    public record NeuronID(int layerID, int neuronID){}

    private RealMatrix outgoingWeights;
    private final List<Connection> connections = new ArrayList<>();

    private final NeuralNetworkLayer ownLayer;

    private final NeuronID identifier;

    public Neuron(NeuronID identifier, NeuralNetworkLayer ownLayer) {
        this.identifier = identifier;
        this.ownLayer = ownLayer;
    }

    public void initializeNeuron(RealMatrix weights, List<Neuron> nextLayerNeurons) {
        this.outgoingWeights = weights;
        for (int i = 0; i < weights.getColumnVector(getIndexInLayer()).getDimension(); i++) {
            this.connections.add(new Connection(this, nextLayerNeurons.get(i), weights));
        }
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public double getBias() {
        return this.ownLayer.getBias().getEntry(this.identifier.neuronID());
    }

    public void setBias(double bias) {
        this.ownLayer.getBias().setEntry(this.identifier.neuronID(), Math.min(Math.max(bias, -1), 1));
    }

    public int getIndexInLayer() {
        return this.identifier.neuronID();
    }

    public NeuronID getIdentifier() {
        return this.identifier;
    }

    public RealVector getOutgoingWeights() {
        return this.outgoingWeights.getColumnVector(getIndexInLayer());
    }

    public void setOutgoingWeights(RealVector outgoingWeights) {
        outgoingWeights.mapToSelf(v -> Math.min(Math.max(v, -1), 1));
        this.outgoingWeights.setColumnVector(getIndexInLayer(), outgoingWeights);
    }

    public Stream<Connection> stream() {
        return this.connections.stream();
    }

    public void forEach(Consumer<? super Connection> consumer) {
        this.connections.forEach(consumer);
    }

    @Override
    public Iterator<Connection> iterator() {
        return this.connections.iterator();
    }

    @Override
    public Spliterator<Connection> spliterator() {
        return this.connections.spliterator();
    }
}
