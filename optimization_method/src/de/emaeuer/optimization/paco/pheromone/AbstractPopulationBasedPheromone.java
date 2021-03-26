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
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;
import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.PACO_SPLIT_PROBABILITY_FUNCTION;

public abstract class AbstractPopulationBasedPheromone {

    public static record FitnessValue(double fitness, double value) {}

    private final ConfigurationHandler<PacoConfiguration> configuration;

    private final int populationSize;

    private final NeuralNetwork baseNetwork;

    // collection containing all ants of this population sorted according to remove strategy
    private final Collection<PacoAnt> population;

    // ranked collection of the ants of this population
    private final TreeSet<PacoAnt> sortedPopulation = new TreeSet<>(Comparator.comparingDouble(PacoAnt::getFitness));

    // a two dimensional table which saves all weights that were assigned to a connection by an ant of this population
    // bag is used to allow duplicates and efficient remove/ add operations
    private final Table<NeuronID, NeuronID, Multiset<FitnessPopulationBasedPheromone.FitnessValue>> weightPheromone = HashBasedTable.create();

    // a one dimensional map which saves all biases that were assigned to a neuron by an ant of this population
    private final Map<NeuronID, Multiset<FitnessPopulationBasedPheromone.FitnessValue>> biasPheromone = new HashMap<>();

    public AbstractPopulationBasedPheromone(ConfigurationHandler<PacoConfiguration> configuration, NeuralNetwork baseNetwork) {
        this.configuration = configuration;
        this.populationSize = this.configuration.getValue(PACO_POPULATION_SIZE, Integer.class);
        this.baseNetwork = baseNetwork;

        this.population = getEmptyPopulation();
    }

    protected abstract Collection<PacoAnt> getEmptyPopulation();

    public void addAntToPopulation(PacoAnt ant) {
        // if the population is completely populated an ant has to be removed before a new one is added
        if (this.populationSize <= this.population.size()) {
            removeAnt();
        }
        addAnt(ant);
    }

    protected abstract PacoAnt removeAndGetAnt();

    private void removeAnt() {
        PacoAnt ant = removeAndGetAnt();
        this.sortedPopulation.remove(ant);

        // remove all weights of this ant
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();
            Collection<FitnessPopulationBasedPheromone.FitnessValue> values = this.weightPheromone.get(next.start(), next.end());
            Objects.requireNonNull(values).remove(new FitnessPopulationBasedPheromone.FitnessValue(ant.getFitness(), next.weight()));
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
                Collection<FitnessPopulationBasedPheromone.FitnessValue> values = this.biasPheromone.get(next);
                values.remove(new FitnessPopulationBasedPheromone.FitnessValue(ant.getFitness(), ant.getNeuralNetwork().getBiasOfNeuron(next)));
                if (values.isEmpty()) {
                    this.biasPheromone.remove(next);
                }
            }
        }
    }

    private void addAnt(PacoAnt ant) {
        population.add(ant);
        this.sortedPopulation.add(ant);

        // add all weights of this ant
        Iterator<Connection> connections = NeuralNetworkUtil.iterateNeuralNetworkConnections(ant.getNeuralNetwork());
        while (connections.hasNext()) {
            Connection next = connections.next();

            if (!this.weightPheromone.contains(next.start(), next.end())) {
                this.weightPheromone.put(next.start(), next.end(), LinkedHashMultiset.create());
            }
            Objects.requireNonNull(this.weightPheromone.get(next.start(), next.end())).add(new FitnessPopulationBasedPheromone.FitnessValue(ant.getFitness(), next.weight()));
        }

        // add all biases of this ant
        Iterator<NeuronID> neurons = NeuralNetworkUtil.iterateNeurons(ant.getNeuralNetwork());
        while (neurons.hasNext()) {
            NeuronID next = neurons.next();
            // ignore unmodifiable input neurons
            if (!ant.getNeuralNetwork().isInputNeuron(next)) {
                this.biasPheromone.putIfAbsent(next, LinkedHashMultiset.create());
                this.biasPheromone.get(next).add(new FitnessPopulationBasedPheromone.FitnessValue(ant.getFitness(), ant.getNeuralNetwork().getBiasOfNeuron(next)));
            }
        }
    }

    public NeuralNetwork createNeuralNetworkForGlobalBest() {
        if (this.population.isEmpty()) {
            return null;
        }
        
        PacoAnt populationBest = getBestAntOfPopulation();
        return populationBest.getNeuralNetwork().copy();
    }

    protected abstract PacoAnt getBestAntOfPopulation();

    public NeuralNetwork createNeuralNetworkForPheromone() {
        // select random ant from this population and use its neural network as prototype
        NeuralNetwork prototype = getPrototypeNeuralNetwork();

        // modify prototype depending on other values of this population
        adjustWeights(prototype);
        adjustBias(prototype);
        addAdditionalConnection(prototype);

        return prototype;
    }

    protected NeuralNetwork getPrototypeNeuralNetwork() {
        // if no knowledge exists start with base network
        if (this.population.isEmpty()) {
            return this.baseNetwork.copy();
        }

        // (rank / sum ranks) is the selection probability for each element
        // TODO use int instead
        double[] rankValues = IntStream.rangeClosed(1, this.sortedPopulation.size())
                .mapToDouble(v -> v)
                .toArray();

        int selectedIndex = RandomUtil.selectRandomElementFromVector(rankValues);

        // start from the end because they have a higher selection probability
        Iterator<PacoAnt> antIterator = this.sortedPopulation.descendingIterator();
        for (int i = this.sortedPopulation.size() - 1; i != selectedIndex && antIterator.hasNext(); i--) {
            // iterate until selected ant is reached
            antIterator.next();
        }

        return Objects.requireNonNull(antIterator.next()).getNeuralNetwork().copy();
    }

    protected void adjustWeights(NeuralNetwork nn) {
        NeuralNetworkUtil.iterateNeuralNetworkConnections(nn).forEachRemaining(c -> calculateWeightValue(nn, c));
    }

    protected void adjustBias(NeuralNetwork nn) {
        NeuralNetworkUtil.iterateNeurons(nn).forEachRemaining(n -> calculateBiasValue(nn, n));
    }

    protected void addAdditionalConnection(NeuralNetwork nn) {
        List<NeuronID> possibleSources = IntStream.range(0, nn.getDepth())
                .mapToObj(nn::getNeuronsOfLayer)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // input neurons can be sources but not targets
        List<NeuronID> possibleTargets =  possibleSources.stream()
                .filter(Predicate.not(nn::isInputNeuron))
                .collect(Collectors.toList());

        for (NeuronID source : possibleSources) {
            for (NeuronID target : possibleTargets) {
                // connections that already exist were handled in the preparation
                if (!nn.neuronHasConnectionTo(source, target)) {
                    Collection<FitnessPopulationBasedPheromone.FitnessValue> populationValues = this.weightPheromone.get(source, target);
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
        double weight = calculateNewValueDependingOnPopulationKnowledge(connection.weight(), this.weightPheromone.get(connection.start(), connection.end()));

        nn.modify().setWeightOfConnection(connection.start(), connection.end(), weight);
    }

    private void calculateBiasValue(NeuralNetwork nn, NeuronID neuron) {
        // ignore unmodifiable input neurons
        if (nn.isInputNeuron(neuron)) {
            return;
        }

        double bias = calculateNewValueDependingOnPopulationKnowledge(nn.getBiasOfNeuron(neuron), this.biasPheromone.get(neuron));

        nn.modify().setBiasOfNeuron(neuron, bias);
    }

    private double calculateNewValueDependingOnPopulationKnowledge(double srcValue, Collection<FitnessPopulationBasedPheromone.FitnessValue> populationValues) {
        double value;

        if (populationValues == null || populationValues.isEmpty()) {
            // choose value randomly because no knowledge exists
            value = RandomUtil.getNextDouble(-1, 1);
        } else {
            double deviation = calculateDeviation(populationValues, srcValue);

            // select new value if it is out of bounds
            do {
                value = RandomUtil.getNormalDistributedValue(srcValue, deviation);
            } while (value < -1 || value > 1);
        }

        return value;
    }

    private double calculateDeviation(Collection<FitnessPopulationBasedPheromone.FitnessValue> populationValues, double mean) {
        double sumOfDifferences = populationValues.stream()
                .mapToDouble(FitnessPopulationBasedPheromone.FitnessValue::value)
                .map(d -> Math.abs(mean - d))
                .sum();

        Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                .with(PacoParameter.POPULATION_SIZE, this.populationSize)
                .with(PacoParameter.NUMBER_OF_VALUES, populationValues.size())
                .with(PacoParameter.SUM_OF_DIFFERENCES, sumOfDifferences)
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

        System.out.println(Arrays.toString(probabilityVector));

        // FIXME new connections with little knowledge about get high scores
        int selectedIndex = RandomUtil.selectRandomElementFromVector(probabilityVector);

        return splitProbabilities.get(selectedIndex);
    }

    private List<Connection> getBaseConnectionsWithSplitProbabilities() {
        List<Connection> splitProbabilities = new ArrayList<>();
        Iterator<Connection> baseConnections = NeuralNetworkUtil.iterateNeuralNetworkConnections(this.baseNetwork);

        while (baseConnections.hasNext()) {
            Connection baseConnection = baseConnections.next();
            Collection<FitnessPopulationBasedPheromone.FitnessValue> populationValues = Objects.requireNonNull(this.weightPheromone.get(baseConnection.start(), baseConnection.end()));

            double averageWeight = populationValues.stream()
                    .mapToDouble(FitnessPopulationBasedPheromone.FitnessValue::value)
                    .average()
                    .orElse(0);

            double sumOfSquares = populationValues.stream()
                    .mapToDouble(FitnessPopulationBasedPheromone.FitnessValue::value)
                    .map(v -> v - averageWeight)
                    .map(v -> Math.pow(v, 2))
                    .sum();

            Map<String, Double> variables = ConfigurationVariablesBuilder.<PacoParameter>build()
                    .with(PacoParameter.POPULATION_SIZE, this.populationSize)
                    .with(PacoParameter.NUMBER_OF_VALUES, populationValues.size())
                    .with(PacoParameter.SUM_OF_DIFFERENCES, sumOfSquares)
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

    public Table<NeuronID, NeuronID, Multiset<FitnessPopulationBasedPheromone.FitnessValue>> getWeightPheromone() {
        return weightPheromone;
    }

    public Map<NeuronID, Multiset<FitnessPopulationBasedPheromone.FitnessValue>> getBiasPheromone() {
        return biasPheromone;
    }

    public int getPopulationSize() {
        return populationSize;
    }
}
