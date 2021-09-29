package de.emaeuer.cli.parameter;

import picocli.CommandLine;

import java.io.File;
@CommandLine.Command(subcommands = {DannacoCliParameter.class, NeatCliParameter.class})
public class CliParameter {

    @CommandLine.Option(names = "--configFile")
    private File configFile;

    @CommandLine.Option(names = "--maxTime", defaultValue = "0")
    private int maxTime;

    @CommandLine.Option(names = "--numberOfRuns", defaultValue = "1")
    private int numberOfRuns;

    @CommandLine.Option(names = "--parallel", defaultValue = "1")
    private int parallel;

    @CommandLine.Option(names = "--seed", defaultValue = "9369319")
    private int seed;

    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(int maxTime) {
        this.maxTime = maxTime;
    }

    public int getNumberOfRuns() {
        return numberOfRuns;
    }

    public void setNumberOfRuns(int numberOfRuns) {
        this.numberOfRuns = numberOfRuns;
    }

    public int getParallel() {
        return parallel;
    }

    public void setParallel(int parallel) {
        this.parallel = parallel;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }
}
