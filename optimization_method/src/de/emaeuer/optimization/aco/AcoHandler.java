package de.emaeuer.optimization.aco;

import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.aco.colony.AcoColony;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.aco.configuration.AcoConfiguration;
import de.emaeuer.optimization.aco.state.AcoState;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.DataSeriesStateValue;
import de.emaeuer.state.value.StateValueFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.aco.configuration.AcoConfiguration.*;

public class AcoHandler extends OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(AcoHandler.class);

    private final List<AcoColony> colonies = new ArrayList<>();

    private final ConfigurationHandler<AcoConfiguration> configuration;
    private final StateHandler<AcoState> state = new StateHandler<>(AcoState.class);

    public AcoHandler(ConfigurationHandler<OptimizationConfiguration> generalConfig, StateHandler<OptimizationState> generalState) {
        super(generalConfig, generalState);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(generalConfig, AcoConfiguration.class, OptimizationConfiguration.OPTIMIZATION_CONFIGURATION);

        // register own state in optimization state
        generalState.addNewValue(OptimizationState.IMPLEMENTATION_STATE, this.state);

        // build basic neural network with just the necessary network neurons and connections
        NeuralNetwork basicNetwork = NeuralNetwork.build()
                .configure(ConfigurationHelper.extractEmbeddedConfiguration(generalConfig, NeuralNetworkConfiguration.class, OptimizationConfiguration.OPTIMIZATION_NEURAL_NETWORK_CONFIGURATION))
                .inputLayer()
                .fullyConnectToNextLayer()
                .outputLayer()
                .finish();

        // create aco colonies
        IntStream.range(0, configuration.getValue(ACO_NUMBER_OF_COLONIES, Integer.class))
                .mapToObj(i -> new AcoColony(basicNetwork.copy(), generalConfig, i))
                .forEach(colonies::add);
    }

    @Override
    public List<? extends Solution> generateSolutions() {
        return this.colonies.stream()
                .map(AcoColony::nextIteration)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void update() {
        this.colonies.forEach(AcoColony::updateSolutions);

        AcoColony worst = this.colonies.get(0);
        AcoColony best = this.colonies.get(0);

        for (AcoColony colony : this.colonies) {
            updateFitnessState(colony);
            worst = colony.getCurrentFitness() < worst.getCurrentFitness() ? colony : worst;
            best = colony.getCurrentFitness() > best.getCurrentFitness() ? colony : best;
        }

        setCurrentlyBestSolution(best.getBestAnt());

        // best is a lot better than worst --> worst gets solution of best
        // TODO threshold for overtake in configuration and add time during overtakes
        if (worst.getCurrentFitness() * 10 < best.getCurrentFitness()) {
            LOG.info("ACO-Colony {} takes the solution of {} because it is significantly worse", worst.getColonyNumber(), best.getColonyNumber());
            worst.takeSolutionOf(best);
        }

        super.update();
    }

    private void updateFitnessState(AcoColony colony) {
        //noinspection unchecked safe cast for generic not possible
        Map<String, AbstractStateValue<?, ?>> currentState = (Map<String, AbstractStateValue<?, ?>>) this.state.getValue(AcoState.COLONY_FITNESS, Map.class);
        String colonyKey = "Colony " + colony.getColonyNumber();

        DataSeriesStateValue series;
        if (currentState.containsKey(colonyKey)) {
            series = (DataSeriesStateValue) currentState.get(colonyKey);
        } else {
            series = (DataSeriesStateValue) StateValueFactory.createValueForClass(DataSeriesStateValue.class);
            currentState.put(colonyKey, series);
        }

        DoubleSummaryStatistics fitnessScores = colony.getIterationStatistic();
        this.state.addNewValue(AcoState.FITNESS_OF_ALL_COLONIES, new AbstractMap.SimpleEntry<>(colonyKey, new Double[]{(double) getEvaluationCounter(), fitnessScores.getMax()}));
        series.newValue(new AbstractMap.SimpleEntry<>(TOTAL_MAX, new Double[]{(double) getEvaluationCounter(), colony.getBestScore()}));
        series.newValue(new AbstractMap.SimpleEntry<>(MAX, new Double[]{(double) getEvaluationCounter(), fitnessScores.getMax()}));
        series.newValue(new AbstractMap.SimpleEntry<>(MIN, new Double[]{(double) getEvaluationCounter(), fitnessScores.getMin()}));
        series.newValue(new AbstractMap.SimpleEntry<>(AVERAGE, new Double[]{(double) getEvaluationCounter(), fitnessScores.getAverage()}));
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        return this.colonies.stream()
                .map(AcoColony::getIterationStatistic)
                .reduce(new DoubleSummaryStatistics(), (total, part) -> {
                    total.combine(part);
                    return total;
                });
    }

}
