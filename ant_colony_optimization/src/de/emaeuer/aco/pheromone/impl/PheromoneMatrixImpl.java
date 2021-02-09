package de.emaeuer.aco.pheromone.impl;

import de.emaeuer.aco.Decision;
import de.emaeuer.aco.configuration.AcoConfiguration;
import de.emaeuer.aco.configuration.AcoParameter;
import de.emaeuer.aco.configuration.AcoParameterNames;
import de.emaeuer.aco.pheromone.PheromoneMatrix;
import de.emaeuer.aco.pheromone.PheromoneMatrixModifier;
import de.emaeuer.ann.NeuronID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

import static de.emaeuer.aco.configuration.AcoConfigurationKeys.*;
import static de.emaeuer.aco.configuration.AcoParameterNames.*;

public class PheromoneMatrixImpl implements PheromoneMatrix {

    private final PheromoneMatrixModifier modifier = new PheromoneMatrixModifierImpl(this);

    private final List<PheromoneMatrixLayer> pheromoneLayers = new ArrayList<>();

    private final RealVector startPheromone;

    private final AcoConfiguration configuration;

    public PheromoneMatrixImpl(int numberOfStarts, AcoConfiguration configuration) {
        this.configuration = configuration;

        this.startPheromone = new ArrayRealVector(numberOfStarts);
        this.startPheromone.mapAddToSelf(configuration.getValue(ACO_INITIAL_PHEROMONE_VALUE));
    }

    @Override
    public RealVector getWeightPheromoneOfNeuron(NeuronID neuron) {
        return pheromoneLayers.get(neuron.getLayerIndex()).getWeightPheromoneOfNeuron(neuron.getNeuronIndex());
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
        AcoParameter parameter = new AcoParameter();
        parameter.setParameterValue(NUMBER_OF_DECISIONS, this.startPheromone.getDimension());
        parameter.setParameterValue(PHEROMONE, this.startPheromone.getEntry(decision.neuronID().getNeuronIndex()));
        double pheromoneValue = this.configuration.getValue(ACO_PHEROMONE_UPDATE_FUNCTION, parameter);

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
        AcoParameter parameter = new AcoParameter();

        // dissipate pheromone of start decision
        this.startPheromone.mapToSelf(p -> {
            parameter.setParameterValue(PHEROMONE, p);
            return this.configuration.getValue(ACO_PHEROMONE_DISSIPATION_FUNCTION, parameter);
        });
        System.out.println(startPheromone);
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
    public AcoConfiguration getConfiguration() {
        return this.configuration;
    }

}
