package de.emaeuer.cli.parameter;

import picocli.CommandLine;

@CommandLine.Command(name = "neat")
public class NeatCliParameter implements AlgorithmCliParameter {

    @CommandLine.Option(names = "-sr")
    private Double survivalRate;

    @CommandLine.Option(names = "-tmt")
    private Boolean topologyMutationType;

    @CommandLine.Option(names = "-k")
    private Integer populationSize;

    @CommandLine.Option(names = "-ec")
    private Double chromosomeCompatibilityExcessCoefficient;

    @CommandLine.Option(names = "-dc")
    private Double chromosomeCompatibilityDisjointCoefficient;

    @CommandLine.Option(names = "-cc")
    private Double chromosomeCompatibilityCommonCoefficient;

    @CommandLine.Option(names = "-st")
    private Double speciationThreshold;

    @CommandLine.Option(names = "-e")
    private Boolean useElitism;

    @CommandLine.Option(names = "-ems")
    private Integer elitismMinSpeciesSize;

    @CommandLine.Option(names = "-r")
    private Boolean useRouletteSelection;

    @CommandLine.Option(names = "-ac")
    private Double addConnectionMutationRate;

    @CommandLine.Option(names = "-an")
    private Double addNeuronMutationRate;

    @CommandLine.Option(names = "-rc")
    private Double removeConnectionMutationRate;

    @CommandLine.Option(names = "-rcw")
    private Double removeConnectionMaxWeight;

    @CommandLine.Option(names = "-rcs")
    private String removeConnectionStrategy;

    @CommandLine.Option(names = "-p")
    private Double pruneMutationRate;

    @CommandLine.Option(names = "-w")
    private Double weightMutationRate;

    @CommandLine.Option(names = "-wd")
    private Double weightMutationDeviation;

    public Double getSurvivalRate() {
        return survivalRate;
    }

    public Boolean getTopologyMutationType() {
        return topologyMutationType;
    }

    public Integer getPopulationSize() {
        return populationSize;
    }

    public Double getChromosomeCompatibilityExcessCoefficient() {
        return chromosomeCompatibilityExcessCoefficient;
    }

    public Double getChromosomeCompatibilityDisjointCoefficient() {
        return chromosomeCompatibilityDisjointCoefficient;
    }

    public Double getChromosomeCompatibilityCommonCoefficient() {
        return chromosomeCompatibilityCommonCoefficient;
    }

    public Double getSpeciationThreshold() {
        return speciationThreshold;
    }

    public Boolean getUseElitism() {
        return useElitism;
    }

    public Integer getElitismMinSpeciesSize() {
        return elitismMinSpeciesSize;
    }

    public Boolean getUseRouletteSelection() {
        return useRouletteSelection;
    }

    public Double getAddConnectionMutationRate() {
        return addConnectionMutationRate;
    }

    public Double getAddNeuronMutationRate() {
        return addNeuronMutationRate;
    }

    public Double getRemoveConnectionMutationRate() {
        return removeConnectionMutationRate;
    }

    public Double getRemoveConnectionMaxWeight() {
        return removeConnectionMaxWeight;
    }

    public String getRemoveConnectionStrategy() {
        return removeConnectionStrategy;
    }

    public Double getPruneMutationRate() {
        return pruneMutationRate;
    }

    public Double getWeightMutationRate() {
        return weightMutationRate;
    }

    public Double getWeightMutationDeviation() {
        return weightMutationDeviation;
    }
}
