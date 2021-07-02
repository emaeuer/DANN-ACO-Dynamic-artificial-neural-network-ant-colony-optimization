package de.emaeuer.irace;

import de.emaeuer.cli.CliLauncher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class IRaceRunner {

    private static final Logger LOG = LogManager.getLogger(IRaceRunner.class);



    public static void main(String[] args) {
        LOG.debug("IRACE-Call-Parameters: " + Arrays.toString(args));
        if (args.length < 5) {
            LOG.warn("\nUsage: ./target-runner.jar <configuration_id> <instance_id> <seed> <instance_path_name> <list of parameters>\n");
            System.exit(1);
        }

//        long configurationID = Long.parseLong(args[0]);
//        long instanceID = Long.parseLong(args[1]);
        int seed = Integer.parseInt(args[2]);
//        String instancePathName = args[3];

        String[] algParameters = Arrays.copyOfRange(args, 4, args.length);

        LOG.debug("Starting optimization");
        CliLauncher algorithmRunner = new CliLauncher(algParameters, seed);
        algorithmRunner.run();
        LOG.debug("Optimization finished after {} milliseconds and cost of {}", algorithmRunner.getTimeMillis(), algorithmRunner.getCost());
        System.out.println((algorithmRunner.getCost()) + " " + algorithmRunner.getTimeMillis());
    }
}
