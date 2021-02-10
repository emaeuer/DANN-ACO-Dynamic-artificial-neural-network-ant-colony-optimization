package de.emaeuer.aco.configuration;

import de.emaeuer.optimization.configuration.ConfigurationValue;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.emaeuer.aco.configuration.AcoConfigurationKeys.*;
import static de.emaeuer.aco.configuration.AcoParameterNames.NUMBER_OF_DECISIONS;
import static de.emaeuer.aco.configuration.AcoParameterNames.PHEROMONE;

public class AcoConfiguration extends OptimizationConfiguration<AcoConfigurationKeys, AcoParameterNames> {

    private static final Map<AcoConfigurationKeys, ConfigurationValue<AcoConfigurationKeys>> DEFAULT_CONFIGURATION = new HashMap<>() {{
        put(ACO_PROGRESSION_THRESHOLD, new ConfigurationValue<>(ACO_PROGRESSION_THRESHOLD, "0.2"));
        put(ACO_PROGRESSION_ITERATIONS, new ConfigurationValue<>(ACO_PROGRESSION_ITERATIONS, "5"));
        put(ACO_CONNECTION_SPLIT_PROBABILITY, new ConfigurationValue<>(ACO_CONNECTION_SPLIT_PROBABILITY, "0.2"));
        put(ACO_NUMBER_OF_COLONIES, new ConfigurationValue<>(ACO_NUMBER_OF_COLONIES, "4"));
        put(ACO_COLONY_SIZE, new ConfigurationValue<>(ACO_COLONY_SIZE, "10"));
        put(ACO_INITIAL_PHEROMONE_VALUE, new ConfigurationValue<>(ACO_INITIAL_PHEROMONE_VALUE, "0.1"));
        put(ACO_STANDARD_DEVIATION_FUNCTION, new ConfigurationValue<>(ACO_STANDARD_DEVIATION_FUNCTION, "(1 - 0.5p) / (10p + 1)", PHEROMONE));
        put(ACO_PHEROMONE_UPDATE_FUNCTION, new ConfigurationValue<>(ACO_PHEROMONE_UPDATE_FUNCTION, "tanh(3np)/tanh(3n)", PHEROMONE, NUMBER_OF_DECISIONS));
        put(ACO_PHEROMONE_DISSIPATION_FUNCTION, new ConfigurationValue<>(ACO_PHEROMONE_DISSIPATION_FUNCTION, "0.9p", PHEROMONE));
    }};

    public AcoConfiguration(List<ConfigurationValue<AcoConfigurationKeys>> configurations) {
        super(configurations);
    }

    @Override
    protected Map<AcoConfigurationKeys, ConfigurationValue<AcoConfigurationKeys>> getDefaultConfiguration() {
        return DEFAULT_CONFIGURATION;
    }
}
