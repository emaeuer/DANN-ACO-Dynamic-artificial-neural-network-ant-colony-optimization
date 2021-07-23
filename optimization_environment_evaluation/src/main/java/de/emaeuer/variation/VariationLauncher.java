package de.emaeuer.variation;

import de.emaeuer.cli.CliLauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VariationLauncher {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("logFilename", "variation" + System.currentTimeMillis());

        MultiVariationParameterIterator iter = new MultiVariationParameterIterator();
        iter.addParameter(new IntegerVariationValue("-m", 10, 50, 20));
//        iter.addParameter(new IntegerVariationValue("-k", 10, 22, 4));

        String configFile = "configurations/variation/flappy_bird_configuration.json";

        List<String> defaultArgs = new ArrayList<>();
        defaultArgs.add("--configFile");
        defaultArgs.add(configFile);
        defaultArgs.add("dannaco");

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        executor.setMaximumPoolSize(1000);

        System.out.println("┌──────────────────────────────────────────┬──────────┬────────┬──────────────┬──────────────┬─────────────┐");
        System.out.printf("│ %40s │ %8s │ %6s │ %12s │ %12s │ %11s │%n", "Konfiguration", "Kosten", "Zeit", "Neurone", "Verbindungen", "Erfolgsrate");
        System.out.println("├──────────────────────────────────────────┼──────────┼────────┼──────────────┼──────────────┼─────────────┤");

        for (List<VariationParameter.StaticParameter<?>> staticParameters : iter) {
            executor.execute(() -> {
                StringBuilder configSummary = new StringBuilder();

                List<String> cliParameters = new ArrayList<>(defaultArgs);
                staticParameters.forEach(p -> {
                    configSummary.append(String.format("[%s = %s]", p.name(), p.value()));
                    cliParameters.add(p.name());
                    cliParameters.add(p.value().toString());
                });

                CliLauncher launcher = new CliLauncher(cliParameters.toArray(new String[0]));
                launcher.run();
                System.out.printf("│ %40s │ %8.1f │ %6.2f │ %12.2f │ %12.2f │ %11.4f │%n", configSummary, launcher.getCost(), launcher.getTimeMillis() / 1000.0, launcher.getNumberOfHiddenNodes(), launcher.getNumberOfConnections(), launcher.getSuccessRate());
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(60, TimeUnit.MINUTES);
        if (finished) {
            System.out.println("└──────────────────────────────────────────┴──────────┴────────┴──────────────┴──────────────┴─────────────┘");
            System.exit(0);
        }

    }

}
