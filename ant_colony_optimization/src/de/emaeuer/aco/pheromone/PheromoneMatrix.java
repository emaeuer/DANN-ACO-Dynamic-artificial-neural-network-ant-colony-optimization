package de.emaeuer.aco.pheromone;

import de.emaeuer.aco.Decision;
import de.emaeuer.aco.configuration.AcoConfiguration;
import de.emaeuer.aco.pheromone.impl.PheromoneMatrixImpl;
import de.emaeuer.aco.pheromone.impl.PheromoneMatrixLayer;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;

public interface PheromoneMatrix {

    static PheromoneMatrix buildForNeuralNetwork(NeuralNetwork network, AcoConfiguration configuration) {
        PheromoneMatrix pheromone = new PheromoneMatrixImpl(network.getNeuronsOfLayer(0).size(), configuration);

        IntStream.range(0, network.getDepth())
                .mapToObj(i -> PheromoneMatrixLayer.buildForNeuralNetworkLayer(network, i, configuration))
                .forEach(pheromone.getLayers()::add);

        return pheromone;
    }

    RealVector getWeightPheromoneOfNeuron(NeuronID neuron);

    RealVector getStartPheromoneValues();

    double getBiasPheromoneOfNeuron(NeuronID neuron);

    void updatePheromone(List<Decision> solution);

    int getNumberOfLayers();

    PheromoneMatrixLayer getLayer(int layerID);

    List<PheromoneMatrixLayer> getLayers();

    PheromoneMatrixModifier modify();

    NeuronID getTargetOfNeuronByIndex(NeuronID neuron, int targetIndex);

    AcoConfiguration getConfiguration();
}
