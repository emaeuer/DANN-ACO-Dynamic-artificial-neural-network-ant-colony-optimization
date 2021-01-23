package de.emaeuer.optimization.aco.pheromone;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuralNetworkLayer;
import de.emaeuer.optimization.aco.Ant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CompositePheromoneMatrix implements Iterable<LayerPheromoneMatrix> {

    private final List<LayerPheromoneMatrix> pheromoneMatrices = new ArrayList<>();

    private CompositePheromoneMatrix() {}

    public static CompositePheromoneMatrix buildForNeuralNetwork(NeuralNetwork nn) {
        CompositePheromoneMatrix pheromoneMatrix = new CompositePheromoneMatrix();
        // create a pheromone matrix with the size of the weight matrix and the initial pheromone value at each position
        nn.stream()
                .map(pheromoneMatrix::buildLayer)
                .filter(Objects::nonNull)
                .forEach(pheromoneMatrix.pheromoneMatrices::add);
        return pheromoneMatrix;
    }

    private LayerPheromoneMatrix buildLayer(NeuralNetworkLayer layer) {
        if (layer.isInputLayer()) {
            return null;
        }
        return new LayerPheromoneMatrix(layer.getWeights(), layer.getBias());
    }

    public void updateSolution(List<Ant.Decision> solution, double quality) {
        // update all layers
        for (int i = 0, j = 1; j < solution.size(); i++, j++) {
           LayerPheromoneMatrix currentPheromones = this.pheromoneMatrices.get(i);
           currentPheromones.update(solution.get(i), solution.get(j), quality);
       }
    }

    public Stream<LayerPheromoneMatrix> stream() {
        return this.pheromoneMatrices.stream();
    }

    public Iterator<LayerPheromoneMatrix> iterator() {
        return this.pheromoneMatrices.iterator();
    }

    public int size() {
        return this.pheromoneMatrices.size();
    }
}
