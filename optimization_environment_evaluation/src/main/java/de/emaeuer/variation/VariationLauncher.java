package de.emaeuer.variation;

import de.emaeuer.cli.CliLauncher;
import de.emaeuer.optimization.configuration.OptimizationRunState;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.population.PopulationUpdateStrategies;
import de.emaeuer.optimization.paco.state.PacoState;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.DistributionStateValue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class VariationLauncher {

    private static BufferedWriter writer;

    public static void main(String[] args) throws InterruptedException, IOException {
        System.setProperty("logFilename", "variation" + System.currentTimeMillis());
        Locale.setDefault(Locale.US);

        writer = new BufferedWriter(new FileWriter("temp/variation.result", StandardCharsets.UTF_8, false));

        MultiVariationParameterIterator iter = new MultiVariationParameterIterator();
//        iter.addParameter(new IntegerVariationValue("-k", 5, 50, 5));
//        iter.addParameter(new IntegerVariationValue("-o", 1, 5, 1));
//        iter.addParameter(new IntegerVariationValue("-m", 5, 150, 1));
//        iter.addParameter(new DoubleVariationParameter("-q", 0.01, 1, 100));
//        iter.addParameter(new DoubleVariationParameter("-u", 0, 1, 101));

//        iter.addParameter(new DoubleVariationParameter("-ac", 0.01, 0.99, 15));
//        iter.addParameter(new DoubleVariationParameter("-bc", 0, 5, 15));
//        iter.addParameter(new DoubleVariationParameter("-cc", 0.01, 0.99, 15));

//        iter.addParameter(new DoubleVariationParameter("-aT", 0.01, 0.99, 15));
//        iter.addParameter(new DoubleVariationParameter("-bT", 0, 5, 15));
//        iter.addParameter(new DoubleVariationParameter("-cT", 0.01, 0.99, 15));

//        iter.addParameter(new DoubleVariationParameter("-d", 0, 5, 25));
//        iter.addParameter(new DoubleVariationParameter("-e", 0, 5, 25));

        iter.addParameter(new DoubleVariationParameter("-z", 0, 5, 15));
        iter.addParameter(new DoubleVariationParameter("-eta", 0, 5, 15));
        iter.addParameter(new DoubleVariationParameter("-t", 0, 5, 15));

//        iter.addParameter(new CategoricalVariationParameter("--updateStrategy", Arrays.asList(PopulationUpdateStrategies.getNames())));
//        iter.addParameter(new CategoricalVariationParameter("", Arrays.asList("--elitism", "")));

        List<String> configFiles = new ArrayList<>();
        configFiles.add("configurations/variation/cart_pole_without_standard.json");
//        configFiles.add("configurations/variation/cart_pole_without_standard2.json");
//        configFiles.add("configurations/variation/cart_pole_without_standard3.json");
//        configFiles.add("configurations/variation/cart_pole_without_standard4.json");
//        configFiles.add("configurations/variation/xor.json");
//        configFiles.add("configurations/variation/xor2.json");
//        configFiles.add("configurations/variation/xor3.json");
//        configFiles.add("configurations/variation/xor4.json");
//        configFiles.add("configurations/variation/flappy_bird_configuration.json");
        configFiles.add("configurations/variation/xor.json");


        List<String> defaultArgs = new ArrayList<>();
        defaultArgs.add("--configFile");
        defaultArgs.add("");
        defaultArgs.add("--maxTime");
        defaultArgs.add("600000");
        defaultArgs.add("dannaco");

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);
        executor.setMaximumPoolSize(10000);

        writeLine("┌──────────────────────────────────────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────┬──────────┬────────┬──────────────┬──────────────┬─────────────┬────────────────────────────────────────────────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────────────────────────┬────────────┐");
        writeLine(String.format("│ %80s │ %70s │ %8s │ %6s │ %12s │ %12s │ %11s │ %90s │ %90s │ %10s │", "Konfiguration", "Testinstanz", "Kosten", "Zeit", "Neurone", "Verbindungen", "Erfolgsrate", "Modifikationen", "Evaluationen", "Std"));
        writeLine("├──────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────┼──────────┼────────┼──────────────┼──────────────┼─────────────┼────────────────────────────────────────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────┴────────────┤");

        for (List<VariationParameter.StaticParameter<?>> staticParameters : iter) {
            for (String configFile : configFiles) {
                executor.execute(() -> {
                    StringBuilder configSummary = new StringBuilder();

                    AtomicReference<Double> sum = new AtomicReference<>(0.0);
                    List<String> cliParameters = new ArrayList<>(defaultArgs);
                    staticParameters.forEach(p -> {
                        configSummary.append(p.toString());

                        if ("-ac".equals(p.name()) || "-cc".equals(p.name())) {
                            sum.getAndUpdate(d -> d + Double.parseDouble(p.value().toString()));
                        }

                        if (!p.name().isBlank()) {
                            cliParameters.add(p.name());
                        }

                        if (!p.value().toString().isBlank()) {
                            cliParameters.add(p.value().toString());
                        }
                    });

                    if (sum.get() > 1) {
                        return;
                    }

                    cliParameters.set(1, configFile);

                    CliLauncher launcher = new CliLauncher(cliParameters.toArray(new String[0]));
                    launcher.run();

                    //noinspection unchecked
                    StateHandler<PacoState> state = launcher.getOptimizationState().getValue(OptimizationState.IMPLEMENTATION_STATE, StateHandler.class);
                    String modificationQuantities = state.getCurrentState().get(PacoState.MODIFICATION_DISTRIBUTION).getExportValue();
                    double deviation = ((DistributionStateValue) state.getCurrentState().get(PacoState.AVERAGE_STANDARD_DEVIATION)).getMean();
                    try {
                        writeLine(String.format("│ %80s │ %70s │ %8.1f │ %6.2f │ %12.2f │ %12.2f │ %11.4f │ %90s │ %90s │ %3.6f |", configSummary, configFile, launcher.getCost(),
                                launcher.getTimeMillis() / 1000.0, launcher.getNumberOfHiddenNodes(), launcher.getNumberOfConnections(),
                                launcher.getSuccessRate(), modificationQuantities, launcher.getAllEvaluations(), deviation));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(60, TimeUnit.MINUTES);
        if (finished) {
            writeLine("└──────────────────────────────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────┴──────────┴────────┴──────────────┴──────────────┴─────────────┴────────────────────────────────────────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────┴────────────┘");
            writer.close();
            System.exit(0);
        }
    }

    public synchronized static void writeLine(String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
        System.out.println(line);
    }

}
