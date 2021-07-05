package de.emaeuer.cli.parameter;

import picocli.CommandLine;

import java.io.File;
@CommandLine.Command(subcommands = {PacoCliParameter.class, NeatCliParameter.class})
public class CliParameter {

    @CommandLine.Option(names = "--configFile")
    private File configFile;

    public File getConfigFile() {
        return configFile;
    }
}
