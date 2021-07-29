package de.emaeuer.variation;

import de.emaeuer.cli.CliLauncher;
import de.emaeuer.optimization.configuration.OptimizationRunState;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.state.StateHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class VariationLauncher {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("logFilename", "variation" + System.currentTimeMillis());

        MultiVariationParameterIterator iter = new MultiVariationParameterIterator();
//        iter.addParameter(new IntegerVariationValue("-m", 3, 21, 3));
//        iter.addParameter(new IntegerVariationValue("-k", 4, 15, 1));
//        iter.addParameter(new IntegerVariationValue("-o", 1, 3, 1));
//        iter.addParameter(new DoubleVariationParameter("-q", 0.01, 0.3, 50));
        iter.addParameter(new DoubleVariationParameter("-ac", 0.001, 0.9, 50));
//        iter.addParameter(new DoubleVariationParameter("-bT", 0.5, 5, 20));
        iter.addParameter(new DoubleVariationParameter("-cc", 0.001, 0.9, 50));

        String configFile = "configurations/variation/xor.json";

        List<String> defaultArgs = new ArrayList<>();
        defaultArgs.add("--configFile");
        defaultArgs.add(configFile);
        defaultArgs.add("--maxTime");
        defaultArgs.add("300000");
        defaultArgs.add("dannaco");
        defaultArgs.add("-bc");
        defaultArgs.add("0.9503");

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);
        executor.setMaximumPoolSize(10000);

        System.out.println("┌──────────────────────────────────────────┬──────────┬────────┬──────────────┬──────────────┬─────────────┬────────────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.printf("│ %40s │ %8s │ %6s │ %12s │ %12s │ %11s │ %90s │%n", "Konfiguration", "Kosten", "Zeit", "Neurone", "Verbindungen", "Erfolgsrate", "Modifikationen");
        System.out.println("├──────────────────────────────────────────┼──────────┼────────┼──────────────┼──────────────┼─────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤");

        for (List<VariationParameter.StaticParameter<?>> staticParameters : iter) {
            executor.execute(() -> {
                StringBuilder configSummary = new StringBuilder();

                AtomicReference<Double> sum = new AtomicReference<>(0.0);
                List<String> cliParameters = new ArrayList<>(defaultArgs);
                staticParameters.forEach(p -> {
                    configSummary.append(String.format("[%s = %s]", p.name(), p.value()));

                    if ("-ac".equals(p.name()) || "-cc".equals(p.name())) {
                        sum.getAndUpdate(d -> d + Double.parseDouble(p.value().toString()));
                    }

                    cliParameters.add(p.name());
                    cliParameters.add(p.value().toString());
                });

                if (sum.get() > 1) {
                    return;
                }

                CliLauncher launcher = new CliLauncher(cliParameters.toArray(new String[0]));
                launcher.run();

                //noinspection unchecked
                StateHandler<PacoState> state = launcher.getOptimizationState().getValue(OptimizationState.IMPLEMENTATION_STATE, StateHandler.class);
                String modificationQuantities = state.getCurrentState().get(PacoState.MODIFICATION_DISTRIBUTION).getExportValue();
                System.out.printf("│ %40s │ %8.1f │ %6.2f │ %12.2f │ %12.2f │ %11.4f │ %90s │%n", configSummary, launcher.getCost(),
                        launcher.getTimeMillis() / 1000.0, launcher.getNumberOfHiddenNodes(), launcher.getNumberOfConnections(),
                        launcher.getSuccessRate(), modificationQuantities);
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(60, TimeUnit.MINUTES);
        if (finished) {
            System.out.println("└──────────────────────────────────────────┴──────────┴────────┴──────────────┴──────────────┴─────────────┴────────────────────────────────────────────────────────────────────────────────────────────┘");
            System.exit(0);
        }

    }

}
