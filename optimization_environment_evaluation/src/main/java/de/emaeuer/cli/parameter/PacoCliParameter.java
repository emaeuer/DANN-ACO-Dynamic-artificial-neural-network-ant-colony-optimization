package de.emaeuer.cli.parameter;

import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "dannaco")
public class PacoCliParameter {

    private static final String STANDARD_DEVIATION_TEMPLATE = "(<delta>s)/(k - 1)+<epsilon>z";
    private static final String PHEROMONE_TEMPLATE = "<alpha>(n/k)^<beta>+<gamma>";
    private static final String SPLIT_PROBABILITY_TEMPLATE = "max((c^<zeta>*t^<eta>)/(<theta>d+1), 0)";

    @CommandLine.Option(names = "-k")
    private Integer populationSize;

    @CommandLine.Option(names = "-o")
    private Integer updatesPerIteration;

    @CommandLine.Option(names = "-m")
    private Integer antsPerIteration;

    @CommandLine.Option(names = "--updateStrategy")
    private String updateStrategy;

    @CommandLine.Option(names = "--elitism")
    private Boolean elitism;

    @CommandLine.Option(names = "--neuronIsolation")
    private Boolean neuronIsolation;

    @CommandLine.Option(names = "--reuseSplitKnowledge")
    private Boolean reuseSplitKnowledge;

    @CommandLine.Option(names = "-q")
    private Double solutionWeightFactor;

    @CommandLine.Option(names = "-aT")
    private Double alphaT;

    @CommandLine.Option(names = "-bT")
    private Double betaT;

    @CommandLine.Option(names = "-cT")
    private Double gammaT;

    @CommandLine.Option(names = "-ac")
    private Double alphaC;

    @CommandLine.Option(names = "-bc")
    private Double betaC;

    @CommandLine.Option(names = "-cc")
    private Double gammaC;

    @CommandLine.Option(names = "-eta")
    private Double eta;

    @CommandLine.Option(names = "-z")
    private Double zeta;

    @CommandLine.Option(names = "-d")
    private Double delta;

    @CommandLine.Option(names = "-e")
    private Double epsilon;

    @CommandLine.Option(names = "-t")
    private Double theta;

    public Boolean isElitism() {
        return elitism;
    }

    public Boolean isNeuronIsolation() {
        return neuronIsolation;
    }

    public Boolean isReuseSplitKnowledge() {
        return reuseSplitKnowledge;
    }

    public Integer getPopulationSize() {
        return populationSize;
    }

    public Integer getUpdatesPerIteration() {
        return updatesPerIteration;
    }

    public Integer getAntsPerIteration() {
        return antsPerIteration;
    }

    public String getTopologyPheromoneFunction() {
        if (this.alphaT == null || this.betaT == null || this.gammaT == null) {
            return null;
        }

        String pheromoneFunction = PHEROMONE_TEMPLATE;
        pheromoneFunction = pheromoneFunction.replace("<alpha>", Double.toString(this.alphaT));
        pheromoneFunction = pheromoneFunction.replace("<beta>", Double.toString(this.betaT));
        pheromoneFunction = pheromoneFunction.replace("<gamma>", Double.toString(this.gammaT));
        return pheromoneFunction;
    }

    public String getConnectionPheromoneFunction() {
        if (this.alphaC == null || this.betaC == null || this.gammaC == null) {
            return null;
        }

        String pheromoneFunction = PHEROMONE_TEMPLATE;
        pheromoneFunction = pheromoneFunction.replace("<alpha>", Double.toString(this.alphaC));
        pheromoneFunction = pheromoneFunction.replace("<beta>", Double.toString(this.betaC));
        pheromoneFunction = pheromoneFunction.replace("<gamma>", Double.toString(this.gammaC));
        return pheromoneFunction;
    }

    public String getStandardDeviationFunction() {
        if (this.delta == null || this.epsilon == null) {
            return null;
        }

        String deviationFunction = STANDARD_DEVIATION_TEMPLATE;
        deviationFunction = deviationFunction.replace("<delta>", Double.toString(this.delta));
        deviationFunction = deviationFunction.replace("<epsilon>", Double.toString(this.epsilon));
        return deviationFunction;
    }

    public String getSplitProbabilityFunction() {
        if (this.zeta == null || this.eta == null || this.theta == null) {
            return null;
        }

        String function = SPLIT_PROBABILITY_TEMPLATE;
        function = function.replace("<zeta>", Double.toString(this.zeta));
        function = function.replace("<eta>", Double.toString(this.eta));
        function = function.replace("<theta>", Double.toString(this.theta));
        return function;
    }

    public String getUpdateStrategy() {
        return updateStrategy;
    }

    public Double getSolutionWeightFactor() {
        return solutionWeightFactor;
    }
}
