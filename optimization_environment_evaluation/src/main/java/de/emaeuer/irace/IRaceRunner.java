package de.emaeuer.irace;

import de.emaeuer.cli.CliLauncher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class IRaceRunner {

    public static void main(String[] args) {
        System.setProperty("logFilename", args[0]);
        Logger log = LogManager.getLogger(IRaceRunner.class);

//        searchInFiles("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\tuning\\execDir\\log");
        log.debug("IRACE-Call-Parameters: " + Arrays.toString(args));
        if (args.length < 5) {
            log.warn("\nUsage: ./target-runner.jar <configuration_id> <instance_id> <seed> <instance_path_name> <list of parameters>\n");
            System.exit(1);
        }

//        long instanceID = Long.parseLong(args[1]);
        int seed = Integer.parseInt(args[2]);
//        String instancePathName = args[3];

        String[] algParameters = Arrays.copyOfRange(args, 4, args.length);

        log.debug("Starting optimization");
        CliLauncher algorithmRunner = new CliLauncher(algParameters, seed);
        algorithmRunner.run();
        log.debug("Optimization finished after {} milliseconds and cost of {}", algorithmRunner.getTimeMillis(), algorithmRunner.getCost());
        System.out.println((algorithmRunner.getCost()) + " " + algorithmRunner.getTimeMillis());
    }

    public static void searchInFiles(String directoryName) {
        File directory = new File(directoryName);
        if (!directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        Arrays.stream(Objects.requireNonNull(files))
                .parallel()
                .forEach(f -> {
                    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                        Optional<String> result = reader.lines()
                                .filter(l -> l.toLowerCase().contains("exception"))
                                .filter(l -> !l.toLowerCase().contains("mapping"))
                                .filter(l -> !l.toLowerCase().contains("unexpected exception in update thread"))
                                .findFirst();

                        if (result.isPresent()) {
                            System.out.println(f.getName());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}
