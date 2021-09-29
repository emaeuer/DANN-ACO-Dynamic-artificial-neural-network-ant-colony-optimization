package de.emaeuer.irace;

import de.emaeuer.cli.CliLauncher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IRaceRunner {

    static {
        System.setProperty("logFilename", "variation" + System.currentTimeMillis());
        Locale.setDefault(Locale.US);
        LOG = LogManager.getLogger(CliLauncher.class);
    }

    private static final Logger LOG;

    public static void main(String[] args) {
        try {
            LOG.debug("IRACE-Call-Parameters: " + Arrays.toString(args));
            if (args.length < 5) {
                LOG.warn("\nUsage: ./target-runner.jar <configuration_id> <instance_id> <seed> <instance_path_name> <list of parameters>\n");
                System.exit(1);
            }

            List<String> algParameters = Arrays.asList(Arrays.copyOfRange(args, 4, args.length));
            algParameters = new ArrayList<>(algParameters);
            algParameters.add(0, "--seed");
            algParameters.add(1, args[2]);

            LOG.debug("Starting optimization");
            CliLauncher algorithmRunner = CliLauncher.startFromArgs(algParameters.toArray(String[]::new));
            algorithmRunner.run();
            LOG.debug("Optimization finished after {} milliseconds and cost of {}", algorithmRunner.getTimeMillis(), algorithmRunner.getCost());
            System.out.println(algorithmRunner.getCost() + " " + algorithmRunner.getTimeMillis());
        } catch (Exception e) {
            if (LOG != null) {
                LOG.warn("Unexpected exception", e);
            }
            System.out.println(Double.POSITIVE_INFINITY + " " + 0);
        }
    }
}
