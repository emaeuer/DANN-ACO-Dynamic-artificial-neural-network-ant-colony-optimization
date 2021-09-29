package de.emaeuer.optimization.util;

import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.data.DataPoint;

import java.util.*;

public class RunDataHandler {

    public record RunSummary(double fitness, int evaluations, int hiddenNodes, int connections) {}

    private static class SimpleDoubleStatistic {
        private double sum = 0;
        private double count = 0;

        public void addValue(double value) {
            count++;
            sum += value;
        }

        public double getAverage() {
            return sum / count;
        }
    }

    private final DoubleSummaryStatistics maxFitness = new DoubleSummaryStatistics();

    private final IntSummaryStatistics neededIterationNumbers = new IntSummaryStatistics();
    private final IntSummaryStatistics hiddenNodeNumber = new IntSummaryStatistics();
    private final IntSummaryStatistics connectionNumber = new IntSummaryStatistics();

    private final Map<Integer, SimpleDoubleStatistic> fitnessSeries = new HashMap<>();
    private final List<Integer> evaluationValues = new ArrayList<>();
    private final List<Double> fitnessMaxValues = new ArrayList<>();

    private int evaluationIndex = 0;
    private final int maxEvaluations;

    private final StateHandler<OptimizationState> generalState;

    public RunDataHandler(StateHandler<OptimizationState> state, int maxEvaluations) {
        this.generalState = state;
        this.maxEvaluations = maxEvaluations;
    }

    public void addSummaryOfRun(RunSummary summary) {
        finishSeries(summary.fitness(), summary.evaluations());

        this.maxFitness.accept(summary.fitness());
        this.neededIterationNumbers.accept(summary.evaluations());
        this.hiddenNodeNumber.accept(summary.hiddenNodes());
        this.connectionNumber.accept(summary.connections());
    }

    public void addFitnessSummary(int evaluationCount, DoubleSummaryStatistics statistic) {
        SimpleDoubleStatistic average;

        if (!this.fitnessSeries.containsKey(evaluationCount)) {
            // no previous run needed that many evaluations --> fitness at this iteration was max value
            average = new SimpleDoubleStatistic();
            this.fitnessMaxValues.forEach(average::addValue);
            this.fitnessSeries.put(evaluationCount, average);
            this.evaluationValues.add(evaluationCount);
        } else {
            average = this.fitnessSeries.get(evaluationCount);
        }

        average.addValue(statistic.getMax());

        Map<String, DataPoint> value = new HashMap<>();
        value.put("Average max fitness", new DataPoint(evaluationCount, average.getAverage()));

        this.generalState.execute(t -> t.addNewValue(OptimizationState.AVERAGE_RUN_FITNESS_SERIES, value));
        this.evaluationIndex++;
    }

    private void finishSeries(double maxFitness, double evaluation) {
        Map<String, DataPoint> value = new HashMap<>();

        if (evaluation < this.maxEvaluations) {
            this.fitnessMaxValues.add(maxFitness);
        }

        // if run finished before others add value of max fitness to all following values
        for (int i = this.evaluationIndex; i < this.evaluationValues.size(); i++) {
            int evaluationCount = this.evaluationValues.get(i);
            SimpleDoubleStatistic average = this.fitnessSeries.get(evaluationCount);
            average.addValue(maxFitness);
            value.put("Average max fitness", new DataPoint(evaluationCount, average.getAverage()));
            this.generalState.execute(t -> t.addNewValue(OptimizationState.AVERAGE_RUN_FITNESS_SERIES, value));
        }

        this.evaluationIndex = 0;
    }
}
