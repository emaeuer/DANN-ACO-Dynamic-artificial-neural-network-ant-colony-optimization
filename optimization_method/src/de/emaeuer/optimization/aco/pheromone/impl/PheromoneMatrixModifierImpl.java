package de.emaeuer.optimization.aco.pheromone.impl;

import de.emaeuer.optimization.aco.pheromone.PheromoneMatrix;
import de.emaeuer.optimization.aco.pheromone.PheromoneMatrixModifier;
import de.emaeuer.ann.NeuronID;

import java.util.stream.IntStream;

public class PheromoneMatrixModifierImpl implements PheromoneMatrixModifier {

    private final PheromoneMatrix matrix;

    public PheromoneMatrixModifierImpl(PheromoneMatrix matrix) {
        this.matrix = matrix;
    }

    @Override
    public PheromoneMatrixModifier splitConnection(NeuronID start, NeuronID end, NeuronID intermediate) {
        // check if new layer was created for intermediate neuron --> is first in its layer
        if (intermediate.getNeuronIndex() == 0) {
            // create new layer for intermediate neuron
            createNewLayerForNeuron(intermediate);
        } else {
            // necessary layers already exists
            addNeuron(intermediate);
        }

        // replace old connection by two new one
        removeConnection(start, end);
        addConnection(start, intermediate);
        addConnection(intermediate, end);

        return this;
    }

    private void createNewLayerForNeuron(NeuronID neuron) {
        PheromoneMatrixLayer layer = PheromoneMatrixLayer.buildLayerWithSingleNeuron(neuron, matrix.getConfiguration());

        // increase layer index of all following layers
        IntStream.range(neuron.getLayerIndex(), matrix.getNumberOfLayers())
                .mapToObj(matrix::getLayer)
                .forEach(l -> l.setLayerIndex(l.getLayerIndex() + 1));

        // add layer at corresponding position
        matrix.getLayers().add(neuron.getLayerIndex(), layer);
    }

    @Override
    public PheromoneMatrixModifier addConnection(NeuronID start, NeuronID end) {
        matrix.getLayer(start.getLayerIndex())
            .addConnection(start, end);

        return this;
    }

    @Override
    public PheromoneMatrixModifier removeConnection(NeuronID start, NeuronID end) {
        matrix.getLayer(start.getLayerIndex())
                .removeConnection(start, end);

        return this;
    }

    @Override
    public PheromoneMatrixModifier addNeuron(NeuronID neuron) {
        matrix.getLayer(neuron.getLayerIndex())
            .addNeuron(neuron);

        return this;
    }

    @Override
    public PheromoneMatrixModifier removeNeuron(NeuronID neuron) {
        matrix.getLayers()
                .forEach(l -> l.removeNeuron(neuron));

        return this;
    }

}
