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

    private final AtomicInteger connectionMappingCounter = new AtomicInteger(0);
    private final Table<Integer, String, Integer> connectionMapping = HashBasedTable.create();
    private final Map<Integer, Multiset<Double>> weightPheromone = new HashMap<>();

    private final Map<String, Integer> templatePheromone = new HashMap<>();

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
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(this.baseNetwork);

        while (connections.hasNext()) {
            String connectionKey = createConnectionKey(connections.next());
            this.connectionMapping.put(this.baseNetwork.getNumberOfHiddenNeurons(), connectionKey, this.connectionMappingCounter.getAndIncrement());
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
        // remove template form list of instances of this template
        String templateKey = createTemplateKey(ant.getNeuralNetwork());
        Integer value = this.templatePheromone.get(templateKey);

        if (value == null || value <= 1) {
            this.templatePheromone.remove(templateKey);
        } else {
            this.templatePheromone.compute(templateKey, (k, v) -> Objects.requireNonNull(v) - 1);
        }
    }

    private void removeWeightsOfAnt(PacoAnt ant) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();
            // checking if population contains value is not necessary because if the value would be missing the procedure for adding
            // ants doesn't work properly --> error is justified
            Multiset<Double> values = getPopulationValues(next.start(), next.end(), ant.getNeuralNetwork());
            values.remove(next.weight());

            if (values.isEmpty()) {
                // if no values exists the collection can be deleted from the map
                removePopulationValues(next.start(), next.end(), ant.getNeuralNetwork());
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
        // add template to list of instances of this template
        String templateKey = createTemplateKey(ant.getNeuralNetwork());
        this.templatePheromone.compute(templateKey, (k, v) -> Objects.requireNonNullElse(v, 0) + 1);
    }

    private void addWeightsOfAnt(PacoAnt ant) {
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        connections.forEachRemaining(c -> addConnectionToPopulation(c, ant.getNeuralNetwork()));
    }

    private void addConnectionToPopulation(Connection connection, NeuralNetwork template) {
        getOrCreatePopulationValues(connection.start(), connection.end(), template)
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

    protected void applyNeuralNetworkDynamics(NeuralNetwork template) {
        if (this.templatePheromone.isEmpty()) {
            return;
        }

        String templateKey = createTemplateKey(template);

        if (templateIsDynamic(templateKey)) {
            Decision dynamicElement = makeDynamicDecision(template, templateKey);

            if (dynamicElement == null) {
                return;
            }

            switch (dynamicElement.type()) {
                case ADD -> addConnection(template, dynamicElement.connection());
                case SPLIT -> splitConnection(template, dynamicElement.connection());
                case REMOVE -> removeConnection(template, dynamicElement.connection());
            }
        }
    }

    private boolean templateIsDynamic(String templateKey) {
        return calculateTopologyPheromone(templateKey) > this.rng.getNextDouble(0, 1);
    }

    private void removeConnection(NeuralNetwork template, Connection dynamicElement) {
        LOG.debug("Removing connection {} to {}", dynamicElement.start(), dynamicElement.end());
        template.modify().removeConnection(dynamicElement.start(), dynamicElement.end());
    }

    private void addConnection(NeuralNetwork template, Connection dynamicElement) {
        LOG.debug("Adding connection {} to {}", dynamicElement.start(), dynamicElement.end());
        template.modify().addConnection(dynamicElement.start(), dynamicElement.end(), 0);
        createMappingsIfNecessary(template, dynamicElement, null);

        // depending on the order of topology and weight adjustment this value may be overwritten
        adjustWeightValue(template, dynamicElement);
    }

    private void splitConnection(NeuralNetwork template, Connection dynamicElement) {
        LOG.debug("Splitting connection {} to {}", dynamicElement.start(), dynamicElement.end());
        NeuronID splitResult = template.modify().splitConnection(dynamicElement.start(), dynamicElement.end()).getLastModifiedNeuron();
        createMappingsIfNecessary(template, dynamicElement, splitResult);
    }

    private Decision makeDynamicDecision(NeuralNetwork template, String templateKey) {
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
                    .map(t -> createDecision(source, t, template, templateKey))
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

    private Decision createDecision(NeuronID source, NeuronID target, NeuralNetwork template, String templateKey) {
        Connection connection = new Connection(source, target, 0);
        double pheromone = calculateConnectionPheromone(connection, template);
        boolean isSplit = isSplit(connection, template, templateKey);
        DecisionType type = null;

        if (!isValidDecision(connection, template, templateKey, isSplit)) {
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

    private boolean isSplit(Connection connection, NeuralNetwork template, String templateKey) {
        if (template.neuronHasConnectionTo(connection.start(), connection.end())) {
            return isSplitInsteadOfRemove(connection, templateKey, template);
        }

        return false;
    }

    private boolean isSplitInsteadOfRemove(Connection connection, String templateKey, NeuralNetwork template) {
        double topologyPheromone = calculateTopologyPheromone(templateKey);
        double connectionPheromone = calculateConnectionPheromone(connection, template);

        Multiset<Double> populationKnowledge = getPopulationValues(connection.start(), connection.end(), template);
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

    private void createMappingsIfNecessary(NeuralNetwork template, Connection dynamicElement, NeuronID splitResult) {
        if (splitResult == null) {
            refreshMappingForNewConnection(template, dynamicElement);
        } else {
            refreshMappingAfterSplit(template, dynamicElement, splitResult);
        }
    }

    private void refreshMappingForNewConnection(NeuralNetwork template, Connection dynamicElement) {
        int templateSize = template.getNumberOfHiddenNeurons();
        String connectionKey = createConnectionKey(dynamicElement);

        if (!this.connectionMapping.contains(templateSize, connectionKey)) {
            // this connection occurred for the first time and must be mapped
            int index = this.connectionMappingCounter.getAndIncrement();
            this.connectionMapping.put(templateSize, connectionKey, index);
        }
    }

    private void refreshMappingAfterSplit(NeuralNetwork template, Connection dynamicElement, NeuronID splitResult) {
        boolean reuseSplitKnowledge = this.configuration.getValue(REUSE_SPLIT_KNOWLEDGE, Boolean.class);

        int newTemplateSize = template.getNumberOfHiddenNeurons();
        int oldTemplateSize = newTemplateSize - 1;

        // check if mappings for new template already exist
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(template);
        while (connections.hasNext()) {
            Connection connection = connections.next();
            String connectionKey = createConnectionKey(connection);

            if (this.connectionMapping.contains(newTemplateSize, connectionKey)) {
                // do nothing because mapping already exists
                continue;
            }

            String dynamicElementConnectionKey = createConnectionKey(dynamicElement);

            int index;

            if (reuseSplitKnowledge && connection.end() == splitResult) {
                // reference the knowledge of the split connection
                index = Objects.requireNonNull(this.connectionMapping.get(oldTemplateSize, dynamicElementConnectionKey),
                        String.format("Mapping for (%d, %s) doesn't exist", oldTemplateSize, connectionKey));
            } else if (!this.connectionMapping.contains(oldTemplateSize, connectionKey)) {
                // the old template doesn't contain the connection key --> it's a completely new connection
                index = this.connectionMappingCounter.getAndIncrement();
            } else {
                // the old template contains the connection key --> use knowledge of existing connection for the new template
                index = Objects.requireNonNull(this.connectionMapping.get(oldTemplateSize, connectionKey),
                        String.format("Mapping for (%d, %s) doesn't exist", oldTemplateSize, connectionKey));
            }

            this.connectionMapping.put(newTemplateSize, connectionKey, index);
        }
    }

    private boolean isValidDecision(Connection connection, NeuralNetwork template, String templateKey, boolean isSplit) {
        if (template.neuronHasConnectionTo(connection.start(), connection.end()) && isSplit) {
            return splitIsValid(connection, template);
        } else if (template.neuronHasConnectionTo(connection.start(), connection.end())) {
            // a connection can't be removed if it is the only output and neuron isolation is forbidden
            return checkNeuronIsolation(connection, template, templateKey);
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

    private boolean checkNeuronIsolation(Connection connection, NeuralNetwork template, String templateKey) {
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

    protected void adjustWeights(NeuralNetwork template) {
        NeuralNetworkUtil.iterateNeuralNetworkConnections(template)
                .forEachRemaining(c -> adjustWeightValue(template, c));
    }

    private void adjustWeightValue(NeuralNetwork template, Connection connection) {
        double weight = calculateNewValueDependingOnPopulationKnowledge(connection.weight(), getPopulationValues(connection.start(), connection.end(), template));

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

    private double calculateTopologyPheromone(String templateKey) {
        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.maximalPopulationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, this.templatePheromone.get(templateKey))
                .getVariables();

        return this.configuration.getValue(TOPOLOGY_PHEROMONE, Double.class, variables);
    }

    protected double calculateConnectionPheromone(Connection connection, NeuralNetwork template) {
        NeuronID start = connection.start();
        NeuronID end = connection.end();

        Multiset<Double> populationKnowledge = getPopulationValues(start, end, template);

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

    public Multiset<Double> getPopulationValues(NeuronID start, NeuronID end, NeuralNetwork template) {
        String connectionKey = createConnectionKey(start, end);
        Integer index = this.connectionMapping.get(template.getNumberOfHiddenNeurons(), connectionKey);

        if (index == null) {
            return null;
        }

        return this.weightPheromone.get(index);
    }

    private void removePopulationValues(NeuronID start, NeuronID end, NeuralNetwork template) {
        String connectionKey = createConnectionKey(start, end);
        // also remove mapping because no value is associated with index
        int index = Objects.requireNonNull(this.connectionMapping.get(template.getNumberOfHiddenNeurons(), connectionKey),
                String.format("Mapping for (%s, %s) doesn't exist", createTemplateKey(template), connectionKey));
        this.weightPheromone.remove(index);
    }

    private Multiset<Double> getOrCreatePopulationValues(NeuronID start, NeuronID end, NeuralNetwork template) {
        String connectionKey = createConnectionKey(start, end);

        Multiset<Double> result = getPopulationValues(start, end, template);
        if (result == null) {
            int index = Objects.requireNonNull(this.connectionMapping.get(template.getNumberOfHiddenNeurons(), connectionKey),
                    String.format("Mapping for (%s, %s) doesn't exist", createTemplateKey(template), connectionKey));
            result = HashMultiset.create();
            this.weightPheromone.put(index, result);
        }

        return result;
    }
}
