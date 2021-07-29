package de.emaeuer.cli.parameter;

import picocli.CommandLine;

import java.io.File;
@CommandLine.Command(subcommands = {DannacoCliParameter.class, NeatCliParameter.class})
public class CliParameter {

    @CommandLine.Option(names = "--configFile")
    private File configFile;

    @CommandLine.Option(names = "--maxTime")
    private int maxTime;

    public File getConfigFile() {
        return configFile;
    }

    public int getMaxTime() {
        return maxTime;
    }
}
