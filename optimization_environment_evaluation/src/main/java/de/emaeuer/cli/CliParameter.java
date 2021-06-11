package de.emaeuer.cli;

import picocli.CommandLine.Option;

import java.io.File;

public class CliParameter {

    @Option(names = "--optimizationConfig")
    private File optimizationConfig;
    @Option(names = "--environmentConfig")
    private File environmentConfig;

    @Option(names = "--populationSize")
    private Integer populationSize;
    @Option(names = "--numberOfUpdates")
    private Integer numberOfUpdates;
    @Option(names = "--antsPerIteration")
    private Integer antsPerIteration;

    @Option(names = "--deviationFunction")
    private String deviationFunction;
    @Option(names = "--populationStrategy")
    private String populationStrategy;
    @Option(names = "--changeProbability")
    private String changeProbability;
    @Option(names = "--pheromoneValue")
    private String pheromoneValue;
    @Option(names = "--spitThreshold")
    private String spitThreshold;

    @Option(names = "--elitism")
    private Boolean elitism;
    @Option(names = "--neuronIsolation")
    private Boolean neuronIsolation;

    public File getOptimizationConfig() {
        return optimizationConfig;
    }

    public void setOptimizationConfig(File optimizationConfig) {
        this.optimizationConfig = optimizationConfig;
    }

    public File getEnvironmentConfig() {
        return environmentConfig;
    }

    public void setEnvironmentConfig(File environmentConfig) {
        this.environmentConfig = environmentConfig;
    }

    public Integer getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public Integer getNumberOfUpdates() {
        return numberOfUpdates;
    }

    public void setNumberOfUpdates(int numberOfUpdates) {
        this.numberOfUpdates = numberOfUpdates;
    }

    public Integer getAntsPerIteration() {
        return antsPerIteration;
    }

    public void setAntsPerIteration(int antsPerIteration) {
        this.antsPerIteration = antsPerIteration;
    }

    public String getDeviationFunction() {
        return deviationFunction;
    }

    public void setDeviationFunction(String deviationFunction) {
        this.deviationFunction = deviationFunction;
    }

    public String getPopulationStrategy() {
        return populationStrategy;
    }

    public void setPopulationStrategy(String populationStrategy) {
        this.populationStrategy = populationStrategy;
    }

    public String getChangeProbability() {
        return changeProbability;
    }

    public void setChangeProbability(String changeProbability) {
        this.changeProbability = changeProbability;
    }

    public String getPheromoneValue() {
        return pheromoneValue;
    }

    public void setPheromoneValue(String pheromoneValue) {
        this.pheromoneValue = pheromoneValue;
    }

    public String getSpitThreshold() {
        return spitThreshold;
    }

    public void setSpitThreshold(String spitThreshold) {
        this.spitThreshold = spitThreshold;
    }

    public Boolean isElitism() {
        return elitism;
    }

    public void setElitism(boolean elitism) {
        this.elitism = elitism;
    }

    public Boolean isNeuronIsolation() {
        return neuronIsolation;
    }

    public void setNeuronIsolation(boolean neuronIsolation) {
        this.neuronIsolation = neuronIsolation;
    }
}
