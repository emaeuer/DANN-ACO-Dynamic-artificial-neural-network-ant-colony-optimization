package de.emaeuer.optimization.paco.pheromone;

import com.google.common.collect.*;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.util.NeuralNetworkUtil;
import de.emaeuer.ann.util.NeuralNetworkUtil.Connection;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariablesBuilder;
import de.emaeuer.optimization.util.RandomUtil;
import de.emaeuer.optimization.paco.PacoAnt;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.configuration.PacoParameter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PopulationBasedPheromone {

    public static record FitnessValue(double fitness, double value) {}

    private final ConfigurationHandler<PacoConfiguration> configuration;

    private final int populationSize;

    private final NeuralNetwork baseNetwork;

    // sorted set of all ants of this population
    private final MinMaxPriorityQueue<PacoAnt> population;

    // a two dimensional table which saves all weights that were assigned to a connection by an ant of this population
    // bag is used to allow duplicates and efficient remove/ add operations
    private final Table<NeuronID, NeuronID, Multiset<FitnessValue>> weightPheromone = HashBasedTable.create();

    // a one dimensional map which saves all biases that were assigned to a neuron by an ant of this population
    private final Map<NeuronID, Multiset<FitnessValue>> biasPheromone = new HashMap<>();

    public PopulationBasedPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        this.configuration = configuration;
        this.populationSize = this.configuration.getValue(PACO_POPULATION_SIZE, Integer.class);
        this.baseNetwork = baseNetwork;

        this.population = MinMaxPriorityQueue.orderedBy(Comparator.comparingDouble(PacoAnt::getFitness))
                .maximumSize(this.populationSize)
                .create();
    }

    public void addAntToPopulation(PacoAnt ant) {
        if (this.populationSize > this.population.size()) { // if the population is not completely populated add the ant regardless of its fitness
            this.population.add(ant);
            addAnt(ant);
        } else if (this.population.peekFirst().getFitness() <= ant.getFitness() && this.populationSize <= this.population.size()) { // if the population is completely populated add ant only if it is better than the worst
            removeWorstAnt();
            population.add(ant);
            addAnt(ant);
        }

    }

    private void removeWorstAnt() {
        PacoAnt ant = this.population.pollFirst();

        // remove all weights of this ant
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();
            Collection<FitnessValue> values = this.weightPheromone.get(next.start(), next.end());
            Objects.requireNonNull(values).remove(new FitnessValue(ant.getFitness(), next.weight()));
            if (values.isEmpty()) {
                this.weightPheromone.remove(next.start(), next.end());
            }
        }

        // remove all biases of this ant
        Iterator<NeuronID> neurons = NeuralNetworkUtil.iterateNeurons(ant.getNeuralNetwork());
        while (neurons.hasNext()) {
            NeuronID next = neurons.next();
            // ignore unmodifiable input neurons
            if (!ant.getNeuralNetwork().isInputNeuron(next)) {
                Collection<FitnessValue> values = this.biasPheromone.get(next);
                values.remove(new FitnessValue(ant.getFitness(), ant.getNeuralNetwork().getBiasOfNeuron(next)));
                if (values.isEmpty()) {
                    this.biasPheromone.remove(next);
                }
            }
        }
    }

    private void addAnt(PacoAnt ant) {
        // add all weights of this ant
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();

            if (!this.weightPheromone.contains(next.start(), next.end())) {
                this.weightPheromone.put(next.start(), next.end(), LinkedHashMultiset.create());
            }
            Objects.requireNonNull(this.weightPheromone.get(next.start(), next.end())).add(new FitnessValue(ant.getFitness(), next.weight()));
        }

        // add all biases of this ant
        Iterator<NeuronID> neurons = NeuralNetworkUtil.iterateNeurons(ant.getNeuralNetwork());
        while (neurons.hasNext()) {
            NeuronID next = neurons.next();
            // ignore unmodifiable input neurons
            if (!ant.getNeuralNetwork().isInputNeuron(next)) {
                this.biasPheromone.putIfAbsent(next, LinkedHashMultiset.create());
                this.biasPheromone.get(next).add(new FitnessValue(ant.getFitness(), ant.getNeuralNetwork().getBiasOfNeuron(next)));
            }
        }
    }

    public NeuralNetwork createNeuralNetworkForPheromone() {
        // start with base neural network to guarantee that every input has a (in-)direct effect on every output
        NeuralNetwork nn = prepareBaseNetwork();

        addAdditionalConnectionsToNetwork(nn);

        return nn;
    }

    /**
     * Iterate over all neurons and connections of the the neural network to calculate a new weight/ bias derived from the current pheromone
     *
     * @return a neural network with adjusted weights and biases derived from the prototype of the paco handler
     */
    private NeuralNetwork prepareBaseNetwork() {
        NeuralNetwork nn = this.baseNetwork.copy();

        NeuralNetworkUtil.iterateNeuralNetworkConnections(nn).forEachRemaining(c -> calculateWeightValue(nn, c));
        NeuralNetworkUtil.iterateNeurons(nn).forEachRemaining(n -> calculateBiasValue(nn, n));

        return nn;
    }

    private void addAdditionalConnectionsToNetwork(NeuralNetwork nn) {
        List<NeuronID> possibleSources = new ArrayList<>(this.biasPheromone.keySet());
        // input neurons can be sources but not targets
        List<NeuronID> possibleTargets =  possibleSources.stream()
                .filter(Predicate.not(this.baseNetwork::isInputNeuron))
                .collect(Collectors.toList());

        for (NeuronID source : possibleSources) {
            for (NeuronID target : possibleTargets) {
                // connections that already exist were handled in the preparation
                if (!nn.neuronHasConnectionTo(source, target)) {
                    Collection<FitnessValue> populationValues = this.weightPheromone.get(source, target);
                    double usagesInPopulation = populationValues == null ? 0 : populationValues.size();

                    Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                            .with(PacoParameter.POPULATION_SIZE, this.populationSize)
                            .with(PacoParameter.NUMBER_OF_VALUES, usagesInPopulation)
                            .getVariables();

                    double selectionProbability = this.configuration.getValue(PACO_ADDITIONAL_CONNECTION_PROBABILITY_FUNCTION, Double.class, variables);

                    if (RandomUtil.getNextDouble(0, 1) < selectionProbability) {
                        nn.modify().addConnection(source, target, 0);
                        calculateWeightValue(nn, new Connection(source, target, 0));
                    }
                }
            }
        }
    }

    private void calculateWeightValue(NeuralNetwork nn, Connection connection) {
        double weight = calculateNewValueDependingOnPopulationKnowledge(this.weightPheromone.get(connection.start(), connection.end()));

        nn.modify().setWeightOfConnection(connection.start(), connection.end(), weight);
    }

    private void calculateBiasValue(NeuralNetwork nn, NeuronID neuron) {
        // ignore unmodifiable input neurons
        if (nn.isInputNeuron(neuron)) {
            return;
        }

        double bias = calculateNewValueDependingOnPopulationKnowledge(this.biasPheromone.get(neuron));

        nn.modify().setBiasOfNeuron(neuron, bias);
    }

    private double calculateNewValueDependingOnPopulationKnowledge(Collection<FitnessValue> populationValues) {
        double value;

        if (populationValues == null || populationValues.isEmpty()) {
            // choose value randomly because no knowledge exists
            value = RandomUtil.getNextDouble(-1, 1);
        } else {
            // choose value depending on existing knowledge from an normal distribution
            double mean = selectMeanFromKnowledge(populationValues);
            double deviation = calculateDeviation(populationValues, mean);

            // select new value if it is out of bounds
            do {
                value = RandomUtil.getNormalDistributedValue(mean, deviation);
            } while (value < -1 || value > 1);
        }

        return value;
    }

    private double selectMeanFromKnowledge(Collection<FitnessValue> populationValues) {
        List<FitnessValue> listOfValues = new ArrayList<>(populationValues);

        double[] fitnessDistribution = listOfValues.stream()
                .mapToDouble(FitnessValue::fitness)
                .toArray();

        int selectedIndex = RandomUtil.selectRandomElementFromVector(fitnessDistribution);
        return listOfValues.get(selectedIndex).value();
    }

    private double calculateDeviation(Collection<FitnessValue> populationValues, double mean) {
        double sumOfSquares = populationValues.stream()
                .mapToDouble(FitnessValue::value)
                .map(d -> Math.abs(mean - d))
                .map(d -> Math.pow(d, 2))
                .sum();

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.populationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, populationValues.size())
                .with(PacoParameter.SUM_OF_SQUARES_FROM_MEAN, sumOfSquares)
                .getVariables();

        return this.configuration.getValue(PACO_DEVIATION_FUNCTION, Double.class, variables);
    }

    public void increaseComplexity() {
        Connection connectionToSplit = determineConnectionToSplit();
        splitConnection(connectionToSplit);
    }

    private Connection determineConnectionToSplit() {
        List<Connection> splitProbabilities = getBaseConnectionsWithSplitProbabilities();

        double[] probabilityVector = splitProbabilities.stream()
                .mapToDouble(Connection::weight)
                .toArray();

        // FIXME new connections with little knowledge about get high scores
        int selectedIndex = RandomUtil.selectRandomElementFromVector(probabilityVector);

        return splitProbabilities.get(selectedIndex);
    }

    private List<Connection> getBaseConnectionsWithSplitProbabilities() {
        List<Connection> splitProbabilities = new ArrayList<>();
        Iterator<Connection> baseConnections = NeuralNetworkUtil.iterateNeuralNetworkConnections(this.baseNetwork);

        while (baseConnections.hasNext()) {
            Connection baseConnection = baseConnections.next();
            Collection<FitnessValue> populationValues = Objects.requireNonNull(this.weightPheromone.get(baseConnection.start(), baseConnection.end()));

            double averageWeight = populationValues.stream()
                    .mapToDouble(FitnessValue::value)
                    .average()
                    .orElse(0);

            double sumOfSquares = populationValues.stream()
                    .mapToDouble(FitnessValue::value)
                    .map(v -> v - averageWeight)
                    .map(v -> Math.pow(v, 2))
                    .sum();

            Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                    .with(PacoParameter.POPULATION_SIZE, this.populationSize)
                    .with(PacoParameter.NUMBER_OF_VALUES, populationValues.size())
                    .with(PacoParameter.SUM_OF_SQUARES_FROM_MEAN, sumOfSquares)
                    .with(PacoParameter.VALUE, averageWeight)
                    .getVariables();

            double splitProbability = this.configuration.getValue(PACO_SPLIT_PROBABILITY_FUNCTION, Double.class, variables);

            splitProbabilities.add(new Connection(baseConnection.start(), baseConnection.end(), splitProbability));
        }

        return splitProbabilities;
    }

    private void splitConnection(Connection connection) {
        System.out.printf("Splitting connection form %s to %s%n", connection.start(), connection.end());

        // create copy of neuron ids because they are modified in the split method
        this.population.forEach(s -> s.getNeuralNetwork().modify().splitConnection(new NeuronID(connection.start()), new NeuronID(connection.end())));

        this.baseNetwork.modify()
                .splitConnection(connection.start(), connection.end());

        List<PacoAnt> populationCopy = new ArrayList<>(this.population);

        // clear the complete knowledge archive
        this.population.clear();
        this.weightPheromone.clear();
        this.biasPheromone.clear();

        // rebuild the knowledge archive from the modified neural networks
        // TODO may remove random ants to increase exploration (either from hole population or just from the corresponding input to the output)
        populationCopy.forEach(this::addAntToPopulation);
    }

    public Collection<PacoAnt> getPopulation() {
        return population;
    }

    public Table<NeuronID, NeuronID, Multiset<FitnessValue>> getWeightPheromone() {
        return weightPheromone;
    }

    public Map<NeuronID, Multiset<FitnessValue>> getBiasPheromone() {
        return biasPheromone;
    }

}
