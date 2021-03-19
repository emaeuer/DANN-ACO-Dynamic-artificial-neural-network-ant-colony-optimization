package de.emaeuer.optimization.aco.pheromone.impl;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariablesBuilder;
import de.emaeuer.optimization.aco.Decision;
import de.emaeuer.optimization.aco.configuration.AcoConfiguration;
import de.emaeuer.optimization.aco.configuration.AcoParameter;
import de.emaeuer.optimization.aco.pheromone.PheromoneMatrix;
import de.emaeuer.optimization.aco.pheromone.PheromoneMatrixModifier;
import de.emaeuer.ann.NeuronID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.emaeuer.optimization.aco.configuration.AcoConfiguration.*;
import static de.emaeuer.optimization.aco.configuration.AcoParameter.NUMBER_OF_DECISIONS;
import static de.emaeuer.optimization.aco.configuration.AcoParameter.PHEROMONE;

public class PheromoneMatrixImpl implements PheromoneMatrix {

    private final PheromoneMatrixModifier modifier = new PheromoneMatrixModifierImpl(this);

    private final List<PheromoneMatrixLayer> pheromoneLayers = new ArrayList<>();

    private final RealVector startPheromone;

    private final ConfigurationHandler<AcoConfiguration> configuration;

    public PheromoneMatrixImpl(int numberOfStarts, ConfigurationHandler<AcoConfiguration> configuration) {
        this.configuration = configuration;

        Map<String, Double> variables = ConfigurationVariablesBuilder.<AcoParameter>build()
                .with(PHEROMONE, 0)
                .with(NUMBER_OF_DECISIONS, numberOfStarts)
                .getVariables();

        this.startPheromone = new ArrayRealVector(numberOfStarts);
        this.startPheromone.mapAddToSelf(configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE, Double.class, variables));
    }

    @Override
    public RealVector getWeightPheromoneOfNeuron(NeuronID neuron) {
        return pheromoneLayers.get(neuron.getLayerIndex()).getWeightPheromoneOfNeuron(neuron.getNeuronIndex());
    }

    @Override
    public double getWeightPheromoneOfConnection(NeuronID start, NeuronID end) {
        return getLayer(start.getLayerIndex()).getPheromoneOfConnection(start, end);
    }

    @Override
    public RealVector getStartPheromoneValues() {
        return this.startPheromone;
    }

    @Override
    public double getBiasPheromoneOfNeuron(NeuronID neuron) {
        return pheromoneLayers.get(neuron.getLayerIndex()).getBiasPheromoneOfNeuron(neuron.getNeuronIndex());
    }

    @Override
    public void updatePheromone(List<Decision> solution) {
        updateStartPheromone(solution.get(0));
        updateWeightAndBiasPheromone(solution.get(0), solution.subList(1, solution.size()));
        dissipatePheromone();
    }

    private void updateStartPheromone(Decision decision) {
        Map<String, Double> variables = ConfigurationVariablesBuilder.<AcoParameter>build()
                .with(PHEROMONE, this.startPheromone.getEntry(decision.neuronID().getNeuronIndex()))
                .with(NUMBER_OF_DECISIONS, this.startPheromone.getDimension())
                .getVariables();

        double pheromoneValue = this.configuration.getValue(ACO_PHEROMONE_UPDATE_FUNCTION, Double.class, variables);

        this.startPheromone.setEntry(decision.neuronID().getNeuronIndex(), pheromoneValue);
    }

    private void updateWeightAndBiasPheromone(Decision startDecision, List<Decision> solution) {
        NeuronID currentPosition = startDecision.neuronID();

        for (Decision currentDecision : solution) {
            updateWeightPheromone(currentPosition, currentDecision.neuronID());
            updateBiasPheromone(currentDecision.neuronID());
            currentPosition = currentDecision.neuronID();
        }
    }

    private void updateWeightPheromone(NeuronID start, NeuronID target) {
        PheromoneMatrixLayer affectedLayer = this.pheromoneLayers.get(start.getLayerIndex());

        // check if decision is new connection
        if (!affectedLayer.getTargetNeurons().contains(target)) {
            modify().addConnection(start, target);
        }

        affectedLayer.updateWeightPheromone(start, target);
    }

    private void updateBiasPheromone(NeuronID neuron) {
        this.pheromoneLayers.get(neuron.getLayerIndex())
                .updateBiasPheromone(neuron);
    }

    private void dissipatePheromone() {
        // dissipate pheromone of start decision
        this.startPheromone.mapToSelf(p -> {
            Map<String, Double> variables = ConfigurationVariablesBuilder.<AcoParameter>build()
                    .with(PHEROMONE, p)
                    .with(NUMBER_OF_DECISIONS, 1)
                    .getVariables();

            return this.configuration.getValue(ACO_PHEROMONE_DISSIPATION_FUNCTION, Double.class, variables);
        });
        // dissipate pheromone of all layers
        this.pheromoneLayers.forEach(PheromoneMatrixLayer::dissipatePheromone);
    }

    @Override
    public int getNumberOfLayers() {
        return this.pheromoneLayers.size();
    }

    @Override
    public PheromoneMatrixLayer getLayer(int layerID) {
        return this.pheromoneLayers.get(layerID);
    }

    @Override
    public List<PheromoneMatrixLayer> getLayers() {
        return this.pheromoneLayers;
    }

    @Override
    public PheromoneMatrixModifier modify() {
        return this.modifier;
    }

    @Override
    public NeuronID getTargetOfNeuronByIndex(NeuronID neuron, int targetIndex) {
        return this.pheromoneLayers.get(neuron.getLayerIndex())
                .getTargetNeurons()
                .get(targetIndex);
    }

    @Override
    public ConfigurationHandler<AcoConfiguration> getConfiguration() {
        return this.configuration;
    }

}
