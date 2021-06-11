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

import java.util.*;
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

    private final AtomicInteger connectionMappingCounter = new AtomicInteger(0);
    private final Table<String, String, Integer> connectionMapping = HashBasedTable.create();
    private final Map<Integer, Multiset<Double>> weightPheromone = new HashMap<>();


    //############################################################
    //################ Methods for initialization ################
    //############################################################

    public PacoPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        this.configuration = configuration;
        this.maximalPopulationSize = this.configuration.getValue(POPULATION_SIZE, Integer.class);
        this.baseNetwork = baseNetwork;
        this.population = PopulationFactory.create(configuration);

        initializeMapping();
    }

    private void initializeMapping() {
        String nnKey = NeuralNetworkUtil.getTopologySummary(this.baseNetwork);
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(this.baseNetwork);

        while (connections.hasNext()) {
            String connectionKey = createConnectionKey(connections.next());
            this.connectionMapping.put(nnKey, connectionKey, this.connectionMappingCounter.getAndIncrement());
        }
    }

    //############################################################
    //############### Methods for pheromone update ###############
    //############################################################

    public void acceptAntsOfThisIteration(List<PacoAnt> ants) {
        List<PacoAnt>[] populationChange = this.population.acceptAntsOfThisIteration(ants);

        // add all new ants
        Optional.ofNullable(populationChange[0])
                .ifPresent(l -> l.forEach(this::addAnt));

        // remove all ants
        Optional.ofNullable(populationChange[1])
                .ifPresent(l -> l.forEach(this::removeAnt));

        this.templatePheromone.forEach((k, v) -> System.out.println(k + " " + v.size()));
    }

    private void removeAnt(PacoAnt ant) {
        // remove all knowledge of this ant
        String templateKey = removeTemplateOfAnt(ant);
        removeWeightsOfAnt(ant, templateKey);
    }

    private String removeTemplateOfAnt(PacoAnt ant) {
        // remove template form list of instances of this template
        String templateKey = createTemplateKey(ant.getNeuralNetwork());
        List<NeuralNetwork> templateList = this.templatePheromone.get(templateKey);

        if (templateList.size() == 1) {
            this.templatePheromone.remove(templateKey);
        } else {
            templateList.remove(ant.getNeuralNetwork());
        }

        return templateKey;
    }

    private void removeWeightsOfAnt(PacoAnt ant, String templateKey) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();
            // checking if population contains value is not necessary because if the value would be missing the procedure for adding
            // ants doesn't work properly --> error is justified
            Multiset<Double> values = getPopulationValues(next.start(), next.end(), templateKey);
            values.remove(next.weight());

            if (values.isEmpty()) {
                // if no values exists the collection can be deleted from the map
                removePopulationValues(next.start(), next.end(), templateKey);
            }
        }
    }

    protected void addAnt(PacoAnt ant) {
        // add all weights of this ant
        String templateKey = addTemplateOfAnt(ant);
        addWeightsOfAnt(ant, templateKey);
    }

    private String addTemplateOfAnt(PacoAnt ant) {
        // add template to list of instances of this template
        String templateKey = createTemplateKey(ant.getNeuralNetwork());
        this.templatePheromone.putIfAbsent(templateKey, new ArrayList<>());
        this.templatePheromone.get(templateKey).add(ant.getNeuralNetwork());
        return templateKey;
    }

    private void addWeightsOfAnt(PacoAnt ant, String templateKey) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        connections.forEachRemaining(c -> addConnectionToPopulation(c, templateKey));
    }

    private void addConnectionToPopulation(Connection connection, String templateKey) {
        getOrCreatePopulationValues(connection.start(), connection.end(), templateKey)
                .add(connection.weight());
    }

    //############################################################
    //########### Methods for solution generation ################
    //############################################################

    public PacoAnt createAntFromPopulation() {
        // select random ant from this population and use its neural network as template
        NeuralNetwork template = selectNeuralNetworkTemplate();
        String templateKey = createTemplateKey(template);

        // modify template depending on other values of this population
        applyNeuralNetworkDynamics(template, templateKey);
        adjustWeights(template, templateKey);

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

    protected void applyNeuralNetworkDynamics(NeuralNetwork template, String templateKey) {
        if (this.templatePheromone.isEmpty()) {
            return;
        }

        List<NeuralNetwork> similarTemplates = this.templatePheromone.get(templateKey);

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
                    Connection connection = new Connection(source, target, 0);
                    decisions.add(connection);
                    pheromoneValues.add(calculatePheromoneValueOfConnection(connection, template, templateKey));
                }
            }

            int decisionIndex = RandomUtil.selectRandomElementFromVector(pheromoneValues.stream().mapToDouble(d -> d).toArray());
            Connection dynamicElement = decisions.get(decisionIndex);

            // if dynamic element already exists --> remove or split else add
            if (template.neuronHasConnectionTo(dynamicElement.start(), dynamicElement.end())) {
                if (isSplitInsteadOfRemove(dynamicElement, templateKey) && splitIsValid(dynamicElement, template)) {
                    System.out.printf("Splitting connection %s to %s%n", dynamicElement.start(), dynamicElement.end());
                    splitConnection(template, templateKey, dynamicElement);
                } else {
                    System.out.printf("Removing connection %s to %s%n", dynamicElement.start(), dynamicElement.end());
                    template.modify().removeConnection(dynamicElement.start(), dynamicElement.end());
                    createMappingsIfNecessary(template, templateKey, dynamicElement, null);
                }
            } else {
                System.out.printf("Adding connection %s to %s%n", dynamicElement.start(), dynamicElement.end());
                template.modify().addConnection(dynamicElement.start(), dynamicElement.end(), 0);
                createMappingsIfNecessary(template, templateKey, dynamicElement, null);
            }
        }
    }

    private boolean splitIsValid(Connection dynamicElement, NeuralNetwork nn) {
        return dynamicElement.start().getLayerIndex() != dynamicElement.end().getLayerIndex() || !nn.isOutputNeuron(dynamicElement.start());
    }

    private void splitConnection(NeuralNetwork template, String templateKey, Connection dynamicElement) {
        NeuronID splitResult = template.modify().splitConnection(dynamicElement.start(), dynamicElement.end()).getLastModifiedNeuron();

        createMappingsIfNecessary(template, templateKey, dynamicElement, splitResult);
    }

    private void createMappingsIfNecessary(NeuralNetwork template, String oldTemplateKey, Connection dynamicElement, NeuronID splitResult) {
        String newTemplateKey = createTemplateKey(template);

        // check if mappings for new template already exist
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(template);
        while (connections.hasNext()) {
            Connection connection = connections.next();
            String connectionKey = createConnectionKey(connection);

            if (this.connectionMapping.contains(newTemplateKey, connectionKey)) {
                continue;
            }

            int index;
            if (!this.connectionMapping.contains(oldTemplateKey, connectionKey)) {
                index = this.connectionMappingCounter.getAndIncrement();
            } else {
                index = Objects.requireNonNull(this.connectionMapping.get(oldTemplateKey, connectionKey),
                        String.format("Mapping for (%s, %s) doesn't exist", oldTemplateKey, connectionKey));
            }
            this.connectionMapping.put(newTemplateKey, connectionKey, index);
        }
    }

    private boolean isSplitInsteadOfRemove(Connection connection, String templateKey) {
        Multiset<Double> populationKnowledge = getPopulationValues(connection.start(), connection.end(), templateKey);
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

    protected double calculatePheromoneValueOfConnection(Connection connection, NeuralNetwork template, String templateKey) {
        NeuronID start = connection.start();
        NeuronID end = connection.end();

        Multiset<Double> populationKnowledge = getPopulationValues(start, end, templateKey);

        int sizeOfPopulationKnowledge = populationKnowledge == null ? 0 : populationKnowledge.size();

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, sizeOfPopulationKnowledge)
                .getVariables();

        double connectionPheromone = this.configuration.getValue(PHEROMONE_VALUE, Double.class, variables);

        if (!this.configuration.getValue(ENABLE_NEURON_ISOLATION, Boolean.class) && template.neuronHasConnectionTo(start, end)) {
            return checkNeuronIsNotIsolated(connection, template, templateKey) ? 1 - connectionPheromone : 0;
        }else if (template.neuronHasConnectionTo(start, end)) {
            return 1 - connectionPheromone;
        } else {
            return connectionPheromone;
        }
    }

    private boolean checkNeuronIsNotIsolated(Connection connection, NeuralNetwork template, String templateKey) {
        NeuronID start = connection.start();
        NeuronID end = connection.end();

        if (isSplitInsteadOfRemove(new Connection(start, end, 0), templateKey)) {
            return true;
        }

        if (template.getOutgoingConnectionsOfNeuron(start).size() == 1) {
            return template.isOutputNeuron(start);
        }

        return template.getIncomingConnectionsOfNeuron(end).size() != 1;
    }

    protected void adjustWeights(NeuralNetwork nn, String templateKey) {
        NeuralNetworkUtil.iterateNeuralNetworkConnections(nn)
                .forEachRemaining(c -> adjustWeightValue(nn, templateKey, c));
    }

    private void adjustWeightValue(NeuralNetwork nn, String templateKey, Connection connection) {
        double weight = calculateNewValueDependingOnPopulationKnowledge(connection.weight(), getPopulationValues(connection.start(), connection.end(), templateKey));

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

    private String createConnectionKey(Connection connection) {
        return createConnectionKey(connection.start(), connection.end());
    }

    private String createConnectionKey(NeuronID start, NeuronID end) {
        return String.format("%d:%d-%d:%d", start.getLayerIndex(), start.getNeuronIndex(),
                end.getLayerIndex(), end.getNeuronIndex());
    }

    private String createTemplateKey(NeuralNetwork nn) {
        return NeuralNetworkUtil.getTopologySummary(nn);
    }

    public void exportPheromoneMatrixState(int evaluationNumber, StateHandler<PacoState> state) {
//        state.lock();
//        //noinspection unchecked safe cast for generic not possible
//        Map<String, AbstractStateValue<?, ?>> currentState = (Map<String, AbstractStateValue<?, ?>>) state.getValue(PacoState.CONNECTION_WEIGHTS_SCATTERED, Map.class);
//
//        for (Map.Entry<Integer, Multiset<Double>> connection : this.weightPheromone.entrySet()) {
//            currentState.putIfAbsent(connection.getKey().toString(), new ScatteredDataStateValue());
//
//            currentState.get(connection.getKey().toString()).newValue(new AbstractMap.SimpleEntry<>(evaluationNumber,
//                    Objects.requireNonNull(connection.getValue())
//                            .stream()
//                            .mapToDouble(d -> d)
//                            .boxed()
//                            .toArray(Double[]::new)));
//        }
//        state.unlock();
    }

    public Multiset<Double> getPopulationValues(NeuronID start, NeuronID end, String templateKey) {
        String connectionKey = createConnectionKey(start, end);
        Integer index = this.connectionMapping.get(templateKey, connectionKey);

        if (index == null) {
            return null;
        }

        return this.weightPheromone.get(index);
    }

    private void removePopulationValues(NeuronID start, NeuronID end, String templateKey) {
        String connectionKey = createConnectionKey(start, end);
        // also remove mapping because no value is associated with index
        int index = Objects.requireNonNull(this.connectionMapping.get(templateKey, connectionKey),
                String.format("Mapping for (%s, %s) doesn't exist", templateKey, connectionKey));
        this.weightPheromone.remove(index);
    }

    private Multiset<Double> getOrCreatePopulationValues(NeuronID start, NeuronID end, String templateKey) {
        String connectionKey = createConnectionKey(start, end);

        Multiset<Double> result = getPopulationValues(start, end, templateKey);
        if (result == null) {
            int index = Objects.requireNonNull(this.connectionMapping.get(templateKey, connectionKey),
                    String.format("Mapping for (%s, %s) doesn't exist", templateKey, connectionKey));
            result = HashMultiset.create();
            this.weightPheromone.put(index, result);
        }

        return result;
    }

    public int getMaximalPopulationSize() {
        return maximalPopulationSize;
    }

    public int getPopulationSize() {
        return this.population.getSize();
    }

}
