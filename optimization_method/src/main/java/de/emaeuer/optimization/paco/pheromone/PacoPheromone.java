package de.emaeuer.optimization.paco.pheromone;

import com.google.common.collect.*;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.impl.neuron.based.Neuron;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetwork;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil.Connection;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariablesBuilder;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.configuration.PacoParameter;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.ScatteredDataStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PacoPheromone {

    private enum DecisionType {
        ADD,
        REMOVE,
        SPLIT
    }

    private static class TopologyGroup {
        private final int id;
        private int size;

        public TopologyGroup(int id, int size) {
            this.id = id;
            this.size = size;
        }

        public int getId() {
            return id;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    private static record Decision(Connection connection, DecisionType type, double pheromoneValue) {}

    private static final Logger LOG = LogManager.getLogger(PacoPheromone.class);

    private final ConfigurationHandler<PacoConfiguration> configuration;

    private final int maximalPopulationSize;

    private final NeuralNetwork baseNetwork;

    private final List<PacoAnt> solutions = new ArrayList<>();

    // use this sorted flag because java has no sorted data structure which supports indexing
    // --> sort solutions list only when necessary
    private boolean solutionsAreSorted = true;

    private double[] solutionWeights;

    private final AtomicInteger topologyGroupCounter = new AtomicInteger(0);
    private final Map<String, TopologyGroup> topologyGroups = new HashMap<>();

    private final AtomicInteger connectionMappingCounter = new AtomicInteger(0);
    private final Table<Integer, String, Integer> connectionMapping = HashBasedTable.create();
    private final Map<Integer, Multiset<Double>> weightPheromone = new HashMap<>();

    private final RandomUtil rng;

    //############################################################
    //################ Methods for initialization ################
    //############################################################

    public PacoPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork, RandomUtil rng) {
        this.configuration = configuration;
        this.maximalPopulationSize = this.configuration.getValue(POPULATION_SIZE, Integer.class);
        this.baseNetwork = baseNetwork;
        this.rng = rng;

        initializeMapping();
        initializeSolutionWeights();
    }

    private void initializeMapping() {
        String topologyKey = NeuralNetworkUtil.getTopologySummary(this.baseNetwork);
        TopologyGroup group = new TopologyGroup(this.topologyGroupCounter.getAndIncrement(), 0);
        this.topologyGroups.put(topologyKey, group);

        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(this.baseNetwork);

        while (connections.hasNext()) {
            String connectionKey = createConnectionKey(connections.next());
            this.connectionMapping.put(group.getId(), connectionKey, this.connectionMappingCounter.getAndIncrement());
        }
    }

    private void initializeSolutionWeights() {
        this.solutionWeights = IntStream.rangeClosed(1, this.maximalPopulationSize)
                .mapToDouble(this::calculateWeightForRank)
                .toArray();
    }

    private double calculateWeightForRank(int rank) {
        double q = this.configuration.getValue(SOLUTION_WEIGHT_FACTOR, Double.class);
        double k = this.maximalPopulationSize;
        return (1 / (q * k * Math.sqrt(2 * Math.PI))) * Math.exp(-1 * (Math.pow(rank - 1, 2) / (2 * Math.pow(q * k, 2))));
    }

    //############################################################
    //############### Methods for pheromone update ###############
    //############################################################

    public void removeAnt(PacoAnt ant) {
        // remove all knowledge of this ant
        TopologyGroup group = removeTemplateOfAnt(ant);
        removeWeightsOfAnt(ant, group);

        this.solutions.remove(ant);
    }

    private TopologyGroup removeTemplateOfAnt(PacoAnt ant) {
        // remove template form list of instances of this template
        TopologyGroup group = getTopologyGroup(ant.getNeuralNetwork());
        group.setSize(group.getSize() - 1);

        return group;
    }

    private void removeWeightsOfAnt(PacoAnt ant, TopologyGroup group) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();
            // checking if population contains value is not necessary because if the value would be missing the procedure for adding
            // ants doesn't work properly --> error is justified
            Multiset<Double> values = getPopulationValues(next.start(), next.end(), group);
            values.remove(next.weight());

            if (values.isEmpty()) {
                // if no values exists the collection can be deleted from the map
                removePopulationValues(next.start(), next.end(), group);
            }
        }
    }

    public void addAnt(PacoAnt ant) {
        // add all weights of this ant
        TopologyGroup group = addTemplateOfAnt(ant);
        addWeightsOfAnt(ant, group);

        this.solutions.add(ant);
        this.solutionsAreSorted = false;
    }

    private TopologyGroup addTemplateOfAnt(PacoAnt ant) {
        TopologyGroup group = getTopologyGroup(ant.getNeuralNetwork());
        group.setSize(group.getSize() + 1);

        return group;
    }

    private void addWeightsOfAnt(PacoAnt ant, TopologyGroup group) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        connections.forEachRemaining(c -> addConnectionToPopulation(c, group));
    }

    private void addConnectionToPopulation(Connection connection, TopologyGroup group) {
        getOrCreatePopulationValues(connection.start(), connection.end(), group)
                .add(connection.weight());
    }

    //############################################################
    //########### Methods for solution generation ################
    //############################################################

    public PacoAnt createAntFromPopulation() {
        // select random ant from this population and use its neural network as template
        NeuralNetwork template = selectNeuralNetworkTemplate();
        TopologyGroup group = getTopologyGroup(template);

        // modify template depending on other values of this population
        TopologyGroup groupAfterModification = applyNeuralNetworkDynamics(template, group);
        adjustWeights(template, groupAfterModification);

        return new PacoAnt(template);
    }

    protected NeuralNetwork selectNeuralNetworkTemplate() {
        if (this.solutions.isEmpty()) {
            return this.baseNetwork.copy();
        }

        double[] weights = this.solutionWeights;
        if (this.solutions.size() < this.maximalPopulationSize) {
            weights = Arrays.copyOf(this.solutionWeights, this.solutions.size());
        }

        int selectedIndex = rng.selectRandomElementFromVector(weights);

        if (!solutionsAreSorted) {
            this.solutions.sort(Comparator.comparingDouble(PacoAnt::getFitness).reversed());
        }

        PacoAnt templateAnt = this.solutions.get(selectedIndex);

        if (templateAnt != null) {
            return templateAnt.getNeuralNetwork().copy();
        } else {
            LOG.warn("Failed to select template");
            return this.baseNetwork.copy();
        }
    }

    protected TopologyGroup applyNeuralNetworkDynamics(NeuralNetwork template, TopologyGroup group) {
        // if the weight pheromone is empty this is the first iteration and there shouldn't be dynamic
        if (this.weightPheromone.isEmpty()) {
            return group;
        }

        if (templateIsDynamic(group)) {
            Decision dynamicElement = makeDynamicDecision(template, group);

            if (dynamicElement == null) {
                return group;
            }

            group = switch (dynamicElement.type()) {
                case ADD -> addConnection(template, dynamicElement.connection(), group);
                case SPLIT -> splitConnection(template, dynamicElement.connection(), group);
                case REMOVE -> removeConnection(template, dynamicElement.connection(), group);
            };
        }

        return group;
    }

    private boolean templateIsDynamic(TopologyGroup group) {
        return calculateTopologyPheromone(group) > this.rng.getNextDouble(0, 1);
    }

    private TopologyGroup removeConnection(NeuralNetwork template, Connection dynamicElement, TopologyGroup group) {
        LOG.debug("Removing connection {} to {}", dynamicElement.start(), dynamicElement.end());
        template.modify().removeConnection(dynamicElement.start(), dynamicElement.end());

        return createMappingsIfNecessary(template, dynamicElement, null, group);
    }

    private TopologyGroup addConnection(NeuralNetwork template, Connection dynamicElement, TopologyGroup group) {
        LOG.debug("Adding connection {} to {}", dynamicElement.start(), dynamicElement.end());
        template.modify().addConnection(dynamicElement.start(), dynamicElement.end(), 0);
        group = createMappingsIfNecessary(template, dynamicElement, null, group);

        // depending on the order of topology and weight adjustment this value may be overwritten
        adjustWeightValue(template, dynamicElement, group);

        return group;
    }

    private TopologyGroup splitConnection(NeuralNetwork template, Connection dynamicElement, TopologyGroup group) {
        LOG.debug("Splitting connection {} to {}", dynamicElement.start(), dynamicElement.end());
        NeuronID splitResult = template.modify().splitConnection(dynamicElement.start(), dynamicElement.end()).getLastModifiedNeuron();

        return createMappingsIfNecessary(template, dynamicElement, splitResult, group);
    }

    private Decision makeDynamicDecision(NeuralNetwork template, TopologyGroup group) {
        List<NeuronID> possibleSources = IntStream.range(0, template.getDepth())
                .mapToObj(template::getNeuronsOfLayer)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // input neurons can be sources but not targets
        List<NeuronID> possibleTargets = possibleSources.stream()
                .filter(Predicate.not(template::isInputNeuron))
                .collect(Collectors.toList());

        List<Decision> decisions = new ArrayList<>();
        List<Double> pheromoneValues = new ArrayList<>();

        for (NeuronID source : possibleSources) {
            possibleTargets.stream()
                    .map(t -> createDecision(source, t, template, group))
                    .filter(d -> d.type() != null)
                    .filter(d -> d.pheromoneValue() > 0)
                    .peek(d -> pheromoneValues.add(d.pheromoneValue()))
                    .forEach(decisions::add);
        }

        if (decisions.isEmpty()) {
            return null;
        }

        int decisionIndex = this.rng.selectRandomElementFromVector(pheromoneValues.stream().mapToDouble(d -> d).toArray());
        return decisions.get(decisionIndex);
    }

    private Decision createDecision(NeuronID source, NeuronID target, NeuralNetwork template, TopologyGroup group) {
        Connection connection = new Connection(source, target, 0);
        double pheromone = calculateConnectionPheromone(connection, group);
        boolean isSplit = isSplit(connection, template, group);
        DecisionType type = null;

        if (!isValidDecision(connection, template, isSplit)) {
            pheromone = 0;
        } else if (template.neuronHasConnectionTo(source, target) && isSplit) {
            // splitting a connection has a lower probability than adding
//            pheromone /= 10;
            type = DecisionType.SPLIT;
        } else if (template.neuronHasConnectionTo(source, target)) {
            // if the connection exists the pheromone for removing is 1 - pheromone value --> connections with a high pheromone value are less likely to be removed
            pheromone = 1 - pheromone;
            type = DecisionType.REMOVE;
        } else {
            // the connection doesn't exist and the pheromone value is the value for adding it
            type = DecisionType.ADD;
        }

        return new Decision(connection, type, pheromone);
    }

    private boolean isSplit(Connection connection, NeuralNetwork template, TopologyGroup group) {
        if (template.neuronHasConnectionTo(connection.start(), connection.end())) {
            return isSplitInsteadOfRemove(connection, group);
        }

        return false;
    }

    private boolean isSplitInsteadOfRemove(Connection connection, TopologyGroup group) {
        double topologyPheromone = calculateTopologyPheromone(group);
        double connectionPheromone = calculateConnectionPheromone(connection, group);

        Multiset<Double> populationKnowledge = getPopulationValues(connection.start(), connection.end(), group);
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
                .with(PacoParameter.CONNECTION_PHEROMONE, connectionPheromone)
                .with(PacoParameter.TOPOLOGY_PHEROMONE, topologyPheromone)
                .getVariables();

        return this.configuration.getValue(SPLIT_PROBABILITY, Double.class, variables) > this.rng.nextDouble();
    }

    private TopologyGroup createMappingsIfNecessary(NeuralNetwork template, Connection dynamicElement, NeuronID splitResult, TopologyGroup group) {
        if (splitResult == null) {
            return refreshMappingForConnectionChange(template, group, dynamicElement);
        } else {
            return refreshMappingAfterSplit(template, dynamicElement, splitResult, group);
        }
    }

    private TopologyGroup refreshMappingForConnectionChange(NeuralNetwork template, TopologyGroup group, Connection dynamicElement) {
        String connectionKey = createConnectionKey(dynamicElement);


        if (!this.topologyGroups.containsKey(connectionKey)) {
            // this template occurred for the first time count is 0 but uses the old group id
            boolean sizeForGroup = this.configuration.getValue(CALCULATE_TOPOLOGY_PHEROMONE_FOR_GROUP, Boolean.class);
            group = sizeForGroup ? group : new TopologyGroup(group.getId(), 0);
            this.topologyGroups.putIfAbsent(createTemplateKey(template), group);
        }

        if (!this.connectionMapping.contains(group.getId(), connectionKey) && template.neuronHasConnectionTo(dynamicElement.start(), dynamicElement.end())) {
            // the dynamic element was added and occurred for the first time
            int index = this.connectionMappingCounter.getAndIncrement();
            this.connectionMapping.put(group.getId(), connectionKey, index);
        }

        return group;
    }

    private TopologyGroup refreshMappingAfterSplit(NeuralNetwork template, Connection dynamicElement, NeuronID splitResult, TopologyGroup oldGroup) {
        String templateKey = createTemplateKey(template);
        TopologyGroup newGroup = getTopologyGroup(templateKey);

        if (newGroup != null) {
            // group already exists --> all mappings should exist too
            return newGroup;
        }

        newGroup = new TopologyGroup(this.topologyGroupCounter.getAndIncrement(), 0);
        this.topologyGroups.put(templateKey, newGroup);

        // create copy because the row is only a view and modifications would be made to the old group
        Map<String, Integer> newMapping = new HashMap<>(this.connectionMapping.row(oldGroup.getId()));

        // remove the mapping of the replaced connection by the two new ones
        int oldConnectionID = newMapping.remove(createConnectionKey(dynamicElement));

        boolean reuseSplitKnowledge = this.configuration.getValue(REUSE_SPLIT_KNOWLEDGE, Boolean.class);
        oldConnectionID = reuseSplitKnowledge ? oldConnectionID : this.connectionMappingCounter.getAndIncrement();
        newMapping.put(createConnectionKey(dynamicElement.start(), splitResult), oldConnectionID);
        newMapping.put(createConnectionKey(splitResult, dynamicElement.end()), this.connectionMappingCounter.getAndIncrement());

        int groupID = newGroup.getId();
        newMapping.forEach((k, v) -> this.connectionMapping.put(groupID, k, v));

        return newGroup;
    }

    private boolean isValidDecision(Connection connection, NeuralNetwork template, boolean isSplit) {
        if (template.neuronHasConnectionTo(connection.start(), connection.end()) && isSplit) {
            return splitIsValid(connection, template);
        } else if (template.neuronHasConnectionTo(connection.start(), connection.end())) {
            // a connection can't be removed if it is the only output and neuron isolation is forbidden
            return checkNeuronIsolation(connection, template);
        } else if (!template.neuronHasConnectionTo(connection.start(), connection.end())) {
            // adding a connection may be invalid because it may be invalid
            return checkConnectionRecurrence(connection, template);
        }

        // not defined cases are always invalid (e.g. split of unused connection)
        return false;
    }

    private boolean splitIsValid(Connection dynamicElement, NeuralNetwork nn) {
        // TODO a split of a connection between output neurons is valid but affords the creation of a hidden neuron
        return dynamicElement.start().getLayerIndex() != dynamicElement.end().getLayerIndex() || !nn.isOutputNeuron(dynamicElement.start());
    }

    private boolean checkNeuronIsolation(Connection connection, NeuralNetwork template) {
        NeuronID start = connection.start();
        NeuronID end = connection.end();

        if (start.equals(end)) {
            // self recurrent connections can always be removed
            return true;
        }

        List<NeuronID> outgoingConnectionsOfStart = template.getOutgoingConnectionsOfNeuron(start);
        // a neuron with only one connection to itself is also isolated
        outgoingConnectionsOfStart.remove(start);

        if (outgoingConnectionsOfStart.size() == 1) {
            // only output neurons don't need outputs
            return template.isOutputNeuron(start);
        }

        List<NeuronID> incomingConnectionsOfEnd = template.getIncomingConnectionsOfNeuron(end);
        // a neuron with only one connection to itself is also isolated
        outgoingConnectionsOfStart.remove(end);

        return incomingConnectionsOfEnd.size() > 1;
    }

    private boolean checkConnectionRecurrence(Connection connection, NeuralNetwork template) {
        if (!template.recurrentIsDisabled()) {
            // all connections are valid
            return true;
        } else if (connection.start().getLayerIndex() != connection.end().getLayerIndex()) {
            // connections to previous layers are not valid in non recurrent nets
            return connection.start().getLayerIndex() < connection.end().getLayerIndex();
        } else if (template instanceof NeuronBasedNeuralNetwork nn) {
            // connections in the same layer might be valid
            Neuron start = nn.getNeuron(connection.start());
            Neuron end = nn.getNeuron(connection.end());

            return start.getRecurrentID() < end.getRecurrentID();
        }
        return false;
    }

    protected void adjustWeights(NeuralNetwork template, TopologyGroup group) {
        NeuralNetworkUtil.iterateNeuralNetworkConnections(template)
                .forEachRemaining(c -> adjustWeightValue(template, c, group));
    }

    private void adjustWeightValue(NeuralNetwork template, Connection connection, TopologyGroup group) {
        double weight = calculateNewValueDependingOnPopulationKnowledge(connection.weight(), getPopulationValues(connection.start(), connection.end(), group));

        template.modify().setWeightOfConnection(connection.start(), connection.end(), weight);
    }

    private double calculateNewValueDependingOnPopulationKnowledge(double srcValue, Collection<Double> populationValues) {
        double value;

        double maxValue = this.baseNetwork.getMaxWeightValue();
        double minValue = this.baseNetwork.getMinWeightValue();

        if (populationValues == null || populationValues.isEmpty()) {
            // choose value randomly because no knowledge exists
            value = this.rng.getNextDouble(minValue, maxValue);
        } else {
            double deviation = calculateDeviation(populationValues, srcValue);
            // select new value if it is out of bounds
            do {
                // repeat until a valid weight was chosen
                value = this.rng.getNormalDistributedValue(srcValue, deviation);
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

    private double calculateTopologyPheromone(TopologyGroup group) {
        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, group.getSize())
                .getVariables();

        return this.configuration.getValue(TOPOLOGY_PHEROMONE, Double.class, variables);
    }

    protected double calculateConnectionPheromone(Connection connection, TopologyGroup group) {
        NeuronID start = connection.start();
        NeuronID end = connection.end();

        Multiset<Double> populationKnowledge = getPopulationValues(start, end, group);

        int sizeOfPopulationKnowledge = populationKnowledge == null ? 0 : populationKnowledge.size();

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, sizeOfPopulationKnowledge)
                .getVariables();

        return this.configuration.getValue(CONNECTION_PHEROMONE, Double.class, variables);
    }

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
        state.execute(s -> {
            //noinspection unchecked safe cast for generic not possible
            Map<String, AbstractStateValue<?, ?>> currentState = (Map<String, AbstractStateValue<?, ?>>) s.getValue(PacoState.CONNECTION_WEIGHTS_SCATTERED, Map.class);

            for (Map.Entry<Integer, Multiset<Double>> connection : this.weightPheromone.entrySet()) {
                currentState.putIfAbsent(connection.getKey().toString(), new ScatteredDataStateValue());
                currentState.get(connection.getKey().toString()).newValue(new AbstractMap.SimpleEntry<>(evaluationNumber,
                        Objects.requireNonNull(connection.getValue())
                                .stream()
                                .mapToDouble(d -> d)
                                .boxed()
                                .toArray(Double[]::new)));
            }
        });

        state.export(PacoState.CONNECTION_WEIGHTS_SCATTERED);
    }

    public Multiset<Double> getPopulationValues(NeuronID start, NeuronID end, TopologyGroup group) {
        String connectionKey = createConnectionKey(start, end);
        Integer index = this.connectionMapping.get(group.getId(), connectionKey);

        if (index == null) {
            return null;
        }

        return this.weightPheromone.get(index);
    }

    private void removePopulationValues(NeuronID start, NeuronID end, TopologyGroup group) {
        String connectionKey = createConnectionKey(start, end);
        // also remove mapping because no value is associated with index
        int index = Objects.requireNonNull(this.connectionMapping.get(group.getId(), connectionKey),
                String.format("Mapping for (%s, %s) doesn't exist", group.getId(), connectionKey));
        this.weightPheromone.remove(index);
    }

    private Multiset<Double> getOrCreatePopulationValues(NeuronID start, NeuronID end, TopologyGroup group) {
        String connectionKey = createConnectionKey(start, end);

        Multiset<Double> result = getPopulationValues(start, end, group);
        if (result == null) {
            int index = Objects.requireNonNull(this.connectionMapping.get(group.getId(), connectionKey),
                    String.format("Mapping for (%s, %s) doesn't exist", group.getId(), connectionKey));
            result = HashMultiset.create();
            this.weightPheromone.put(index, result);
        }

        return result;
    }

    private TopologyGroup getTopologyGroup(NeuralNetwork nn) {
        String topologyKey = createTemplateKey(nn);
        return this.topologyGroups.get(topologyKey);
    }

    private TopologyGroup getTopologyGroup(String topologyKey) {
        return this.topologyGroups.get(topologyKey);
    }
}
