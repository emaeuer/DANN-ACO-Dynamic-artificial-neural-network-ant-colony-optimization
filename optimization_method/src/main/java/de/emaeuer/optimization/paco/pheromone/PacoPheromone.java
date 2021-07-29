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
import de.emaeuer.optimization.TopologyData;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.configuration.PacoParameter;
import de.emaeuer.optimization.paco.state.PacoRunState;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.data.DataPoint;
import de.emaeuer.state.value.ScatteredDataStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PacoPheromone {

    private enum DecisionType {
        ADD,
        REMOVE,
        SPLIT,
        NOTHING
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
    private final Table<Integer, String, Integer> topologyGroupSuccessors = HashBasedTable.create();
    private final Table<Integer, String, Integer> topologyPheromone = HashBasedTable.create();

    private final AtomicLong connectionMappingCounter = new AtomicLong(0);
    private final Table<Integer, String, Long> connectionMapping = HashBasedTable.create();
    private final Map<Long, Multiset<Double>> weightPheromone = new HashMap<>();

    private final RandomUtil rng;

    private final Map<String, Long> modificationCounts = new HashMap<>(4);

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
        List<NeuronID> possibleSources = IntStream.range(0, this.baseNetwork.getDepth())
                .mapToObj(this.baseNetwork::getNeuronsOfLayer)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // input neurons can be sources but not targets
        List<NeuronID> possibleTargets = possibleSources.stream()
                .filter(Predicate.not(this.baseNetwork::isInputNeuron))
                .collect(Collectors.toList());

        int groupID = this.topologyGroupCounter.getAndIncrement();

        for (NeuronID possibleSource : possibleSources) {
            for (NeuronID possibleTarget : possibleTargets) {
                Connection connection = new Connection(possibleSource, possibleTarget, 0);
                if (checkConnectionRecurrence(connection, this.baseNetwork)) {
                    String connectionKey = createConnectionKey(connection);
                    this.connectionMapping.put(groupID, connectionKey, this.connectionMappingCounter.getAndIncrement());
                }
            }
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
        removeTemplateOfAnt(ant);
        removeWeightsOfAnt(ant);

        this.solutions.remove(ant);
    }

    private void removeTemplateOfAnt(PacoAnt ant) {
        // decrease counter of usage of this specific population
        TopologyData antData = ant.getTopologyData();

        Integer oldValue = this.topologyPheromone.get(antData.getTopologyGroupID(), antData.getTopologyKey());

        if (oldValue == null) {
            LOG.error("Tried to remove ant which isn't be in the population");
        } else if (oldValue == 1) {
            this.topologyPheromone.remove(antData.getTopologyGroupID(), antData.getTopologyKey());
        } else {
            this.topologyPheromone.put(antData.getTopologyGroupID(), antData.getTopologyKey(), oldValue - 1);
        }
    }

    private void removeWeightsOfAnt(PacoAnt ant) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();
            // checking if population contains value is not necessary because if the value would be missing the procedure for adding
            // ants doesn't work properly --> error is justified
            Multiset<Double> values = getPopulationValues(next.start(), next.end(), ant.getTopologyData().getTopologyGroupID());
            values.remove(next.weight());

            if (values.isEmpty()) {
                // if no values exists the collection can be deleted from the map
                removePopulationValues(next.start(), next.end(), ant.getTopologyData().getTopologyGroupID());
            }
        }
    }

    public void addAnt(PacoAnt ant) {
        // add all weights of this ant
        addTemplateOfAnt(ant);
        addWeightsOfAnt(ant);

        this.solutions.add(ant);
        this.solutionsAreSorted = false;
    }

    private void addTemplateOfAnt(PacoAnt ant) {
        // increase counter of usage of this specific population
        TopologyData antData = ant.getTopologyData();

        Integer oldValue = this.topologyPheromone.get(antData.getTopologyGroupID(), antData.getTopologyKey());

        if (oldValue == null) {
            this.topologyPheromone.put(antData.getTopologyGroupID(), antData.getTopologyKey(), 1);
        } else {
            this.topologyPheromone.put(antData.getTopologyGroupID(), antData.getTopologyKey(), oldValue + 1);
        }
    }

    private void addWeightsOfAnt(PacoAnt ant) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        connections.forEachRemaining(c -> addConnectionToPopulation(c, ant.getTopologyData().getTopologyGroupID()));
    }

    private void addConnectionToPopulation(Connection connection, int groupID) {
        getOrCreatePopulationValues(connection.start(), connection.end(), groupID)
                .add(connection.weight());
    }

    //############################################################
    //########### Methods for solution generation ################
    //############################################################

    public PacoAnt createAntFromPopulation() {
        // select random ant from this population and use its neural network as template
        PacoAnt templateAnt = selectTemplate();
        TopologyData topology = templateAnt.getTopologyData().copy();

        // modify template depending on other values of this population
        applyNeuralNetworkDynamics(topology);
        adjustWeights(topology);

        return new PacoAnt(topology);
    }

    protected PacoAnt selectTemplate() {
        if (this.solutions.isEmpty()) {
            return new PacoAnt(this.baseNetwork.copy(), 0);
        }

        double[] weights = this.solutionWeights;
        if (this.solutions.size() < this.maximalPopulationSize) {
            weights = Arrays.copyOf(this.solutionWeights, this.solutions.size());
        }

        int selectedIndex = rng.selectRandomElementFromVector(weights);

        if (!solutionsAreSorted) {
            this.solutions.sort(Comparator.comparingDouble(PacoAnt::getGeneralizationCapability)
                    .thenComparingDouble(PacoAnt::getFitness)
                    .reversed());
        }

        PacoAnt templateAnt = this.solutions.get(selectedIndex);

        if (templateAnt != null) {
            return templateAnt;
        } else {
            LOG.warn("Failed to select template");
            return new PacoAnt(this.baseNetwork.copy(), 0);
        }
    }

    protected void applyNeuralNetworkDynamics(TopologyData topology) {
        // if the weight pheromone is empty this is the first iteration and there shouldn't be dynamic
        if (this.weightPheromone.isEmpty()) {
            return;
        }

        if (templateIsDynamic(topology)) {
            Decision dynamicElement = makeDynamicDecision(topology);

            if (dynamicElement == null) {
                return;
            }

            this.modificationCounts.compute(dynamicElement.type.name(), (k, v) -> Objects.requireNonNullElse(v, 0L) + 1);

            switch (dynamicElement.type()) {
                case ADD -> addConnection(topology, dynamicElement.connection());
                case SPLIT -> splitConnection(topology, dynamicElement.connection());
                case REMOVE -> removeConnection(topology, dynamicElement.connection());
            }
        } else {
            this.modificationCounts.compute(DecisionType.NOTHING.name(), (k, v) -> Objects.requireNonNullElse(v, 0L) + 1);
        }
    }

    private boolean templateIsDynamic(TopologyData topology) {
        return calculateTopologyPheromone(topology) > this.rng.getNextDouble(0, 1);
    }

    private void removeConnection(TopologyData topology, Connection dynamicElement) {
        LOG.debug("Removing connection {} to {} in group {}", dynamicElement.start(), dynamicElement.end(), topology.getTopologyGroupID());
        topology.getInstance().modify().removeConnection(dynamicElement.start(), dynamicElement.end());
        topology.refreshTopologyKey();
    }

    private void addConnection(TopologyData topology, Connection dynamicElement) {
        LOG.debug("Adding connection {} to {} in group {}", dynamicElement.start(), dynamicElement.end(), topology.getTopologyGroupID());
        topology.getInstance().modify().addConnection(dynamicElement.start(), dynamicElement.end(), 0);
        topology.refreshTopologyKey();

        // depending on the order of topology and weight adjustment this value may be overwritten
        adjustWeightValue(topology, dynamicElement);
    }

    private void splitConnection(TopologyData topology, Connection dynamicElement) {
        LOG.debug("Splitting connection {} to {} in group {}", dynamicElement.start(), dynamicElement.end(), topology.getTopologyGroupID());
        NeuronID splitResult = topology.getInstance().modify().splitConnection(dynamicElement.start(), dynamicElement.end()).getLastModifiedNeuron();
        topology.refreshTopologyKey();

        createMappingsIfNecessary(topology, dynamicElement, splitResult);
    }

    private Decision makeDynamicDecision(TopologyData topology) {
        NeuralNetwork template = topology.getInstance();

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
                    .map(t -> createDecision(new Connection(source, t, 0), topology))
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

    private Decision createDecision(Connection connection, TopologyData topology) {
        double pheromone = calculateConnectionPheromone(connection, topology.getTopologyGroupID());
        boolean isSplit = isSplit(connection, topology);
        DecisionType type = null;

        NeuralNetwork template = topology.getInstance();

        if (!isValidDecision(connection, template, isSplit)) {
            pheromone = 0;
        } else if (template.neuronHasConnectionTo(connection.start(), connection.end()) && isSplit) {
            pheromone = 1 - pheromone;
            type = DecisionType.SPLIT;
        } else if (template.neuronHasConnectionTo(connection.start(), connection.end())) {
            // if the connection exists the pheromone for removing is 1 - pheromone value --> connections with a high pheromone value are less likely to be removed
            pheromone = 1 - pheromone;
            type = DecisionType.REMOVE;
        } else {
            // the connection doesn't exist and the pheromone value is the value for adding it
            type = DecisionType.ADD;
        }

        return new Decision(connection, type, pheromone);
    }

    private boolean isSplit(Connection connection, TopologyData topology) {
        if (topology.getInstance().neuronHasConnectionTo(connection.start(), connection.end())) {
            return isSplitInsteadOfRemove(connection, topology);
        }

        return false;
    }

    private boolean isSplitInsteadOfRemove(Connection connection, TopologyData topology) {
        int groupID = topology.getTopologyGroupID();

        double topologyPheromone = calculateTopologyPheromone(topology);
        double connectionPheromone = calculateConnectionPheromone(connection, groupID);

        Multiset<Double> populationKnowledge = getPopulationValues(connection.start(), connection.end(), groupID);
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
        } else {
            populationKnowledge = HashMultiset.create();
        }

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, sizeOfPopulationKnowledge)
                .with(PacoParameter.SUM_OF_DIFFERENCES, sumOfDifferences)
                .with(PacoParameter.CONNECTION_PHEROMONE, connectionPheromone)
                .with(PacoParameter.TOPOLOGY_PHEROMONE, topologyPheromone)
                .with(PacoParameter.STANDARD_DEVIATION, calculateDeviation(populationKnowledge, connection.weight()))
                .getVariables();

        return this.configuration.getValue(SPLIT_PROBABILITY, Double.class, variables) > this.rng.nextDouble();
    }

    private void createMappingsIfNecessary(TopologyData topology, Connection dynamicElement, NeuronID splitResult) {
        String changedConnectionKey = createConnectionKey(dynamicElement);
        int oldGroupID = topology.getTopologyGroupID();

        if (this.topologyGroupSuccessors.contains(oldGroupID, changedConnectionKey)) {
            // the split was already performed for this topology group
            topology.setTopologyGroupID(Objects.requireNonNull(this.topologyGroupSuccessors.get(oldGroupID, changedConnectionKey)));
            return;
        }

        topology.setTopologyGroupID(this.topologyGroupCounter.getAndIncrement());

        // register newGroup as successor of old group
        this.topologyGroupSuccessors.put(oldGroupID, changedConnectionKey, topology.getTopologyGroupID());

        // create copy because the row is only a view and modifications would alter the mapping of the parent group
        Map<String, Long> newMapping = new HashMap<>(this.connectionMapping.row(oldGroupID));

        // replace the id of the split connection with a new one (knowledge is not used in this topology or transferred to new connection
        Long oldConnectionID = newMapping.put(changedConnectionKey, this.connectionMappingCounter.getAndIncrement());

        createMappingsForNewConnections(topology.getInstance(), newMapping, splitResult);

        if (this.configuration.getValue(REUSE_SPLIT_KNOWLEDGE, Boolean.class)) {
            newMapping.put(createConnectionKey(dynamicElement.start(), splitResult), oldConnectionID);
        }

        newMapping.forEach((k, v) -> this.connectionMapping.put(topology.getTopologyGroupID(), k, v));
    }

    private void createMappingsForNewConnections(NeuralNetwork template, Map<String, Long> newMapping, NeuronID splitResult) {
        Iterator<NeuronID> otherNeurons = NeuralNetworkUtil.iterateNeurons(template);
        while (otherNeurons.hasNext()) {
            NeuronID neuron = otherNeurons.next();
            Connection connection = new Connection(neuron, splitResult, 0);

            if (checkConnectionRecurrence(connection, template)) {
                String connectionKey = createConnectionKey(connection);
                newMapping.put(connectionKey, this.connectionMappingCounter.getAndIncrement());
            }

            connection = new Connection(splitResult, neuron, 0);
            if (!template.isInputNeuron(neuron) && !neuron.equals(splitResult) && checkConnectionRecurrence(connection, template)) {
                String connectionKey = createConnectionKey(connection);
                newMapping.put(connectionKey, this.connectionMappingCounter.getAndIncrement());
            }
        }
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

        if (start.equals(end) || this.configuration.getValue(ENABLE_NEURON_ISOLATION, Boolean.class)) {
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

    protected void adjustWeights(TopologyData topology) {
        NeuralNetworkUtil.iterateNeuralNetworkConnections(topology.getInstance())
                .forEachRemaining(c -> adjustWeightValue(topology, c));
    }

    private void adjustWeightValue(TopologyData topology, Connection connection) {
        double weight = calculateNewValueDependingOnPopulationKnowledge(connection.weight(), getPopulationValues(connection.start(), connection.end(), topology.getTopologyGroupID()));

        topology.getInstance().modify().setWeightOfConnection(connection.start(), connection.end(), weight);
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

    //############################################################
    //#################### Util Methods ##########################
    //############################################################

    private double calculateDeviation(Collection<Double> populationValues, double mean) {
        double sumOfDifferences = populationValues.stream()
                .mapToDouble(d -> d)
                .map(d -> Math.abs(mean - d))
                .sum();

        ConfigurationVariablesBuilder<PacoParameter> builder = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, populationValues.size())
                .with(PacoParameter.SUM_OF_DIFFERENCES, sumOfDifferences);

        double penalty = this.configuration.getValue(NUMBER_OF_VALUES_PENALTY, Double.class, builder.getVariables());

        builder.with(PacoParameter.NUMBER_OF_VALUES_PENALTY, penalty);

        return this.configuration.getValue(DEVIATION_FUNCTION, Double.class, builder.getVariables());
    }

    private double calculateTopologyPheromone(TopologyData topology) {
        int numberOfUsages = Objects.requireNonNullElse(this.topologyPheromone.get(topology.getTopologyGroupID(), topology.getTopologyKey()), 0);

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, numberOfUsages)
                .getVariables();

        return this.configuration.getValue(TOPOLOGY_PHEROMONE, Double.class, variables);
    }

    protected double calculateConnectionPheromone(Connection connection, int groupID) {
        NeuronID start = connection.start();
        NeuronID end = connection.end();

        Multiset<Double> populationKnowledge = getPopulationValues(start, end, groupID);

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

    public void exportPheromoneMatrixState(int evaluationNumber, StateHandler<PacoRunState> state) {
        state.execute(s -> {
            //noinspection unchecked safe cast for generic not possible
            Map<String, AbstractStateValue<?, ?>> currentState = (Map<String, AbstractStateValue<?, ?>>) s.getValue(PacoRunState.CONNECTION_WEIGHTS_SCATTERED, Map.class);

            for (Map.Entry<Long, Multiset<Double>> connection : this.weightPheromone.entrySet()) {
                currentState.putIfAbsent(connection.getKey().toString(), new ScatteredDataStateValue());
                currentState.get(connection.getKey().toString()).newValue(new AbstractMap.SimpleEntry<>(evaluationNumber,
                        Objects.requireNonNull(connection.getValue())
                                .stream()
                                .mapToDouble(d -> d)
                                .boxed()
                                .toArray(Double[]::new)));
            }

            s.export(PacoRunState.CONNECTION_WEIGHTS_SCATTERED);
        });
    }

    public void exportCurrentGroups(int evaluationNumber, StateHandler<PacoRunState> state) {
        Map<String, DataPoint> value = new HashMap<>();

        for (Integer groupID : this.topologyPheromone.rowKeySet()) {
            double groupUsage = this.topologyPheromone.row(groupID)
                    .values()
                    .stream()
                    .mapToDouble(Integer::doubleValue)
                    .sum();

            value.put(Integer.toString(groupID), new DataPoint(evaluationNumber, groupUsage));
        }

        state.execute(s -> s.addNewValue(PacoRunState.USED_GROUPS, value));
    }

    public void exportModificationCounts(StateHandler<PacoState> state) {
        state.execute(s -> s.addNewValue(PacoState.MODIFICATION_DISTRIBUTION, this.modificationCounts));
        this.modificationCounts.clear();
    }

    public Multiset<Double> getPopulationValues(NeuronID start, NeuronID end, int groupID) {
        String connectionKey = createConnectionKey(start, end);
        Long index = this.connectionMapping.get(groupID, connectionKey);

        if (index == null) {
            return null;
        }

        return this.weightPheromone.get(index);
    }

    private void removePopulationValues(NeuronID start, NeuronID end, int groupID) {
        String connectionKey = createConnectionKey(start, end);
        // also remove mapping because no value is associated with index
        long index = Objects.requireNonNull(this.connectionMapping.get(groupID, connectionKey),
                String.format("Mapping for (%s, %s) doesn't exist", groupID, connectionKey));
        this.weightPheromone.remove(index);
    }

    private Multiset<Double> getOrCreatePopulationValues(NeuronID start, NeuronID end, int groupID) {
        String connectionKey = createConnectionKey(start, end);

        Multiset<Double> result = getPopulationValues(start, end, groupID);
        if (result == null) {
            long index = Objects.requireNonNull(this.connectionMapping.get(groupID, connectionKey),
                    String.format("Mapping for (%s, %s) doesn't exist", groupID, connectionKey));

            result = HashMultiset.create();
            this.weightPheromone.put(index, result);
        }

        return result;
    }
}
