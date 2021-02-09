package de.emaeuer.aco.configuration;

import de.emaeuer.optimization.configuration.ConfigurationKey;

public enum AcoConfigurationKeys implements ConfigurationKey {
    NN_INPUT_LAYER_SIZE,
    NN_OUTPUT_LAYER_SIZE,
    NN_RANDOM_INITIALIZE,

    ACO_PROGRESSION_THRESHOLD,
    ACO_PROGRESSION_ITERATIONS,
    ACO_CONNECTION_SPLIT_PROBABILITY,
    ACO_NUMBER_OF_COLONIES,
    ACO_COLONY_SIZE,
    ACO_INITIAL_PHEROMONE_VALUE,
    ACO_STANDARD_DEVIATION_FUNCTION,
    ACO_PHEROMONE_UPDATE_FUNCTION,
    ACO_PHEROMONE_DISSIPATION_FUNCTION


}
