package de.emaeuer.variation;

import de.emaeuer.cli.CliLauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ParallelLauncher {


    public static void main(String[] args) throws InterruptedException {
        System.setProperty("logFilename", "variation" + System.currentTimeMillis());
        Locale.setDefault(Locale.US);

        Random rng = new Random(984675);

        List<String> defaultArgs = new ArrayList<>();
        defaultArgs.add("--configFile");
        defaultArgs.add("configurations/best_found/aco_complete_cart.json");
        defaultArgs.add("--maxTime");
        defaultArgs.add("1200000");
        defaultArgs.add("dannaco");

        int numberOfRuns = 300;
        int numberOfThreads = 25;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads);
        executor.setMaximumPoolSize(Math.max(numberOfRuns, numberOfThreads));

        for (int i = 0; i < numberOfRuns; i++) {
            executor.execute(() -> {
                CliLauncher algorithmRunner = new CliLauncher(defaultArgs.toArray(String[]::new), rng.nextInt());
                algorithmRunner.run();
//                System.out.println(algorithmRunner.getEvaluations() + "," + algorithmRunner.getSuccessRate() + "," + algorithmRunner.getNumberOfConnections() + "," + algorithmRunner.getNumberOfHiddenNodes());
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(600, TimeUnit.MINUTES);

        if (finished) {
            System.exit(0);
        }

    }
}
