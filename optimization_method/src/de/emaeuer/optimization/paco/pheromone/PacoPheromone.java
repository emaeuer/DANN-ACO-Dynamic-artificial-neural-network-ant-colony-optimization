package de.emaeuer.optimization.paco.pheromone;

import com.google.common.collect.*;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil.Connection;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariablesBuilder;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.configuration.PacoParameter;
import de.emaeuer.optimization.paco.population.AbstractPopulation;
import de.emaeuer.optimization.paco.population.PopulationFactory;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.ScatteredDataStateValue;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PacoPheromone {

    private final ConfigurationHandler<PacoConfiguration> configuration;

    private final int maximalPopulationSize;

    private final NeuralNetwork baseNetwork;

    private final AbstractPopulation<?> population;

    private final Map<String, List<NeuralNetwork>> templatePheromone = new HashMap<>();

    // a two dimensional table which saves all weights that were assigned to a connection by an ant of this population
    // bag is used to allow duplicates and efficient remove/ add operations
    private final Table<Integer, Integer, Multiset<Double>> weightPheromone = HashBasedTable.create();

    // table which contains the mappings for neurons to the id used in weight pheromone
    // keys are the neuron of the network and the depth of the network
    private final Table<NeuronID, Integer, Integer> neuronMapping = HashBasedTable.create();
    private final AtomicInteger neuronMappingCounter = new AtomicInteger(0);

    //############################################################
    //################ Methods for initialization ################
    //############################################################

    public PacoPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        this.configuration = configuration;
        this.maximalPopulationSize = this.configuration.getValue(POPULATION_SIZE, Integer.class);
        this.baseNetwork = baseNetwork;
        this.population = PopulationFactory.create(configuration);

        initializeNeuronMapping();
    }

    private void initializeNeuronMapping() {
        int depth = this.baseNetwork.getDepth();
        NeuralNetworkUtil.iterateNeurons(this.baseNetwork)
                .forEachRemaining(n -> this.neuronMapping.put(n, depth, this.neuronMappingCounter.getAndIncrement()));
    }

    //############################################################
    //############### Methods for pheromone update ###############
    //############################################################

    public void addAntToPopulation(PacoAnt ant) {
        // if the population adds the ant (added ant is returned) adjust the pheromone accordingly
        this.population.addAnt(ant)
                .ifPresent(this::addAnt);
        // if the population removes an ant (ant to remove is present) adjust the pheromone accordingly
        this.population.removeAnt()
                .ifPresent(this::removeAnt);

        this.templatePheromone.forEach((k, v) -> System.out.println(k + " " + v.size()));
    }

    private void removeAnt(PacoAnt ant) {
        // remove all knowledge of this ant
        removeTemplateOfAnt(ant);
        removeWeightsOfAnt(ant);
    }

    private void removeTemplateOfAnt(PacoAnt ant) {
        // remove template form list of instances of this template
        String templateKey = NeuralNetworkUtil.getTopologySummary(ant.getNeuralNetwork());
        List<NeuralNetwork> templateList = this.templatePheromone.get(templateKey);

        if (templateList.size() == 1) {
            this.templatePheromone.remove(templateKey);
        } else {
            // remove by reference because all templates in this list are equal
            templateList.remove(ant.getNeuralNetwork());
        }
    }

    private void removeWeightsOfAnt(PacoAnt ant) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();
            // check if population contains value not necessary because if the value would be missing the procedure for adding
            // ants doesn't work properly --> error is justified
            Collection<Double> values = getPopulationValues(next.start(), next.end(), ant.getNeuralNetwork());
            values.remove(next.weight());

            if (values.isEmpty()) {
                // if no values exists the cell can be deleted from the table
                removePopulationValues(next.start(), next.end(), ant.getNeuralNetwork());
            }
        }
    }

    protected void addAnt(PacoAnt ant) {
        // add all weights of this ant
        addTemplateOfAnt(ant);
        addWeightsOfAnt(ant);
    }

    private void addTemplateOfAnt(PacoAnt ant) {
        // add template to list of instances of this template
        String templateKey = NeuralNetworkUtil.getTopologySummary(ant.getNeuralNetwork());
        this.templatePheromone.putIfAbsent(templateKey, new ArrayList<>());
        this.templatePheromone.get(templateKey).add(ant.getNeuralNetwork());
    }

    private void addWeightsOfAnt(PacoAnt ant) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        connections.forEachRemaining(c -> addConnectionToPopulation(c, ant));
    }

    private void addConnectionToPopulation(Connection connection, PacoAnt ant) {
        getOrCreatePopulationValues(connection.start(), connection.end(), ant.getNeuralNetwork())
                .add(connection.weight());
    }

    //############################################################
    //########### Methods for solution generation ################
    //############################################################

    public PacoAnt createAntFromPopulation() {
        // select random ant from this population and use its neural network as template
        NeuralNetwork template = selectNeuralNetworkTemplate();

        // modify template depending on other values of this population
        applyNeuralNetworkDynamics(template);
        adjustWeights(template);

        return new PacoAnt(template);
    }

    protected NeuralNetwork selectNeuralNetworkTemplate() {
        if (this.templatePheromone.isEmpty()) {
            return this.baseNetwork.copy();
        }

        List<List<NeuralNetwork>> templates = new ArrayList<>(this.templatePheromone.size());
        List<Integer> selectionProbabilities = new ArrayList<>(this.templatePheromone.size());

        this.templatePheromone.forEach((k, v) -> {
            templates.add(v);
            selectionProbabilities.add(v.size());
        });

        int templateIndex = RandomUtil.selectRandomElementFromVector(selectionProbabilities.stream().mapToInt(i -> i).toArray());
        // every instance of the selected template has the same probability
        int[] instanceProbabilities = IntStream.range(0, templates.get(templateIndex).size())
                .map(i -> 1)
                .toArray();
        int instanceIndex = RandomUtil.selectRandomElementFromVector(instanceProbabilities);

        return templates.get(templateIndex).get(instanceIndex).copy();
    }

    protected void applyNeuralNetworkDynamics(NeuralNetwork template) {
        if (this.templatePheromone.isEmpty()) {
            return;
        }

        List<NeuralNetwork> similarTemplates = this.templatePheromone.get(NeuralNetworkUtil.getTopologySummary(template));

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, similarTemplates.size())
                .getVariables();

        // if many ants use this template it is more dynamic
        if (this.configuration.getValue(DYNAMIC_PROBABILITY, Double.class, variables) > RandomUtil.getNextDouble(0, 1)) {
            List<NeuronID> possibleSources = IntStream.range(0, template.getDepth())
                    .mapToObj(template::getNeuronsOfLayer)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            // input neurons can be sources but not targets
            List<NeuronID> possibleTargets = possibleSources.stream()
                    .filter(Predicate.not(template::isInputNeuron))
                    .collect(Collectors.toList());

            List<Connection> decisions = new ArrayList<>();
            List<Double> pheromoneValues = new ArrayList<>();

            for (NeuronID source : possibleSources) {
                for (NeuronID target : possibleTargets) {
                    decisions.add(new Connection(source, target, 0));
                    pheromoneValues.add(calculatePheromoneValueOfConnection(source, target, template));
                }
            }

            int decisionIndex = RandomUtil.selectRandomElementFromVector(pheromoneValues.stream().mapToDouble(d -> d).toArray());
            Connection dynamicElement = decisions.get(decisionIndex);

            // if dynamic element already exists --> remove or split else add
            if (template.neuronHasConnectionTo(dynamicElement.start(), dynamicElement.end())) {
                if (isSplitInsteadOfRemove(dynamicElement, template) && splitIsValid(dynamicElement, template)) {
                    System.out.printf("Splitting connection %s to %s%n", dynamicElement.start(), dynamicElement.end());
                    splitConnection(template, dynamicElement);
                } else {
                    System.out.printf("Removing connection %s to %s%n", dynamicElement.start(), dynamicElement.end());
                    template.modify().removeConnection(dynamicElement.start(), dynamicElement.end());
                }
            } else {
                System.out.printf("Adding connection %s to %s%n", dynamicElement.start(), dynamicElement.end());
                template.modify().addConnection(dynamicElement.start(), dynamicElement.end(), 0);
            }
        }
    }

    private boolean splitIsValid(Connection dynamicElement, NeuralNetwork nn) {
        return dynamicElement.start().getLayerIndex() != dynamicElement.end().getLayerIndex() || !nn.isOutputNeuron(dynamicElement.start());
    }

    private void splitConnection(NeuralNetwork template, Connection dynamicElement) {
        int depth = template.getDepth();

        // create a map with the current indices mapped to the neurons of the not modified neuron
        // the neurons are modified now but the indices should stay the same
        Map<Integer, NeuronID> oldMapping = new HashMap<>();
        NeuralNetworkUtil.iterateNeurons(template)
                .forEachRemaining(n -> oldMapping.put(getIDForNeuron(n, depth), n));

        NeuronID splitResult = template.modify().splitConnection(dynamicElement.start(), dynamicElement.end()).getLastModifiedNeuron();

        // don't use old depth because the split may have created new layers
        int depthAfterSplit = template.getDepth();
        updateNeuronIDMapping(oldMapping, splitResult, depthAfterSplit);
    }

    private void updateNeuronIDMapping(Map<Integer, NeuronID> oldMappings, NeuronID splitResult, int depth) {
        // neurons in a new layer point to the same population knowledge --> knowledge is interchangeable between all topologies
        for (Entry<Integer, NeuronID> oldMapping : oldMappings.entrySet()) {
            if (!this.neuronMapping.contains(oldMapping.getValue(), depth)) {
                this.neuronMapping.put(oldMapping.getValue(), depth, oldMapping.getKey());
            }
        }

        // create mapping for the new neuron if it wasn't mapped already
        if (!this.neuronMapping.contains(splitResult, depth)) {
            this.neuronMapping.put(splitResult, depth, this.neuronMappingCounter.getAndIncrement());
        }
    }

    private boolean isSplitInsteadOfRemove(Connection connection, NeuralNetwork template) {
        int depth = template.getDepth();
        Collection<Double> populationKnowledge = this.weightPheromone.get(getIDForNeuron(connection.start(), depth), getIDForNeuron(connection.end(), depth));
        int sizeOfPopulationKnowledge = populationKnowledge == null ? 0 : populationKnowledge.size();

        double sumOfDifferences = 0;

        if (populationKnowledge != null) {
            double mean = populationKnowledge.stream()
                    .mapToDouble(d -> d)
                    .average()
                    .orElse(0);

            sumOfDifferences = populationKnowledge.stream()
                    .mapToDouble(d -> d)
                    .map(d -> Math.abs(mean - d))
                    .sum();
        }

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, sizeOfPopulationKnowledge)
                .with(PacoParameter.SUM_OF_DIFFERENCES, sumOfDifferences)
                .getVariables();

        return this.configuration.getValue(SPLIT_THRESHOLD, Boolean.class, variables);
    }

    protected double calculatePheromoneValueOfConnection(NeuronID source, NeuronID target, NeuralNetwork template) {
        int depth = template.getDepth();
        int startID = getIDForNeuron(source, depth);
        int endID = getIDForNeuron(target, depth);

        Collection<Double> populationKnowledge = this.weightPheromone.get(startID, endID);

        int sizeOfPopulationKnowledge = populationKnowledge == null ? 0 : populationKnowledge.size();

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, sizeOfPopulationKnowledge)
                .getVariables();

        double connectionPheromone = this.configuration.getValue(PHEROMONE_VALUE, Double.class, variables);

        if (!this.configuration.getValue(ENABLE_NEURON_ISOLATION, Boolean.class) && template.neuronHasConnectionTo(source, target)) {
            return checkNeuronIsNotIsolated(source, target, template) ? 1 - connectionPheromone : 0;
        }else if (template.neuronHasConnectionTo(source, target)) {
            return 1 - connectionPheromone;
        } else {
            return connectionPheromone;
        }
    }

    private boolean checkNeuronIsNotIsolated(NeuronID source, NeuronID target, NeuralNetwork template) {
        if (isSplitInsteadOfRemove(new Connection(source, target, 0), template)) {
            return true;
        }

        if (template.getOutgoingConnectionsOfNeuron(source).size() == 1) {
            return template.isOutputNeuron(source);
        }

        return template.getIncomingConnectionsOfNeuron(target).size() != 1;
    }

    protected void adjustWeights(NeuralNetwork nn) {
        NeuralNetworkUtil.iterateNeuralNetworkConnections(nn)
                .forEachRemaining(c -> adjustWeightValue(nn, c));
    }

    private void adjustWeightValue(NeuralNetwork nn, Connection connection) {
        double weight = calculateNewValueDependingOnPopulationKnowledge(connection.weight(), getPopulationValues(connection.start(), connection.end(), nn));

        nn.modify().setWeightOfConnection(connection.start(), connection.end(), weight);
    }

    private double calculateNewValueDependingOnPopulationKnowledge(double srcValue, Collection<Double> populationValues) {
        double value;

        double maxValue = this.baseNetwork.getMaxWeightValue();
        double minValue = this.baseNetwork.getMinWeightValue();

        if (populationValues == null || populationValues.isEmpty()) {
            // choose value randomly because no knowledge exists
            value = RandomUtil.getNextDouble(minValue, maxValue);
        } else {
            double deviation = calculateDeviation(populationValues, srcValue);
            // select new value if it is out of bounds
            do {
                value = RandomUtil.getNormalDistributedValue(srcValue, deviation);
            } while (value < minValue || value > maxValue);
        }

        return value;
    }

    private double calculateDeviation(Collection<Double> populationValues, double mean) {
        double sumOfDifferences = populationValues.stream()
                .mapToDouble(d -> d)
                .map(d -> Math.abs(mean - d))
                .sum();

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, populationValues.size())
                .with(PacoParameter.SUM_OF_DIFFERENCES, sumOfDifferences)
                .getVariables();

        return this.configuration.getValue(DEVIATION_FUNCTION, Double.class, variables);
    }

    //############################################################
    //#################### Util Methods ##########################
    //############################################################

    public void exportPheromoneMatrixState(int evaluationNumber, StateHandler<PacoState> state) {
        state.lock();
        //noinspection unchecked safe cast for generic not possible
        Map<String, AbstractStateValue<?, ?>> currentState = (Map<String, AbstractStateValue<?, ?>>) state.getValue(PacoState.CONNECTION_WEIGHTS_SCATTERED, Map.class);

        for (Table.Cell<Integer, Integer, Multiset<Double>> connection : this.weightPheromone.cellSet()) {
            int startIndex = Optional.ofNullable(connection.getRowKey()).orElse(-1);
            int endIndex = Optional.ofNullable(connection.getColumnKey()).orElse(-1);

            String id = startIndex + " -> " + endIndex;
            currentState.putIfAbsent(id, new ScatteredDataStateValue());

            currentState.get(id).newValue(new AbstractMap.SimpleEntry<>(evaluationNumber,
                    Objects.requireNonNull(connection.getValue())
                            .stream()
                            .mapToDouble(d -> d)
                            .boxed()
                            .toArray(Double[]::new)));
        }
        state.unlock();
    }

    public Multiset<Double> getPopulationValues(NeuronID start, NeuronID end, NeuralNetwork nn) {
        int depth = nn.getDepth();
        return this.weightPheromone.get(getIDForNeuron(start, depth), getIDForNeuron(end, depth));
    }

    private void removePopulationValues(NeuronID start, NeuronID end, NeuralNetwork nn) {
        int depth = nn.getDepth();
        this.weightPheromone.remove(getIDForNeuron(start, depth), getIDForNeuron(end, depth));
    }

    private Multiset<Double> getOrCreatePopulationValues(NeuronID start, NeuronID end, NeuralNetwork nn) {
        int depth = nn.getDepth();
        Multiset<Double> result = getPopulationValues(start, end, nn);
        if (result == null) {
            result = HashMultiset.create();
            this.weightPheromone.put(getIDForNeuron(start, depth), getIDForNeuron(end, depth), result);
        }
        return result;
    }

    protected int getIDForNeuron(NeuronID neuron, int depth) {
        return Objects.requireNonNull(this.neuronMapping.get(neuron, depth));
    }

    public int getMaximalPopulationSize() {
        return maximalPopulationSize;
    }

    public int getPopulationSize() {
        return this.population.getSize();
    }
}
