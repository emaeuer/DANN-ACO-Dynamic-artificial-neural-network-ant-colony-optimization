package de.emaeuer.optimization.neat.mapping;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.neat.configuration.NeatConfiguration;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationPropertyMapper {

    private static final Map<String, String> KEY_MAPPINGS = new HashMap<>() {

        @Serial
        private static final long serialVersionUID = 3352223092752399805L;

        {
            put(enumToKey(OptimizationConfiguration.MAX_NUMBER_OF_EVALUATIONS), "num.generations");
            put(enumToKey(NeatConfiguration.POPULATION_SIZE), "popul.size");
            put(enumToKey(NeatConfiguration.TOPOLOGY_MUTATION_CLASSIC), "topology.mutation.classic");
            put(enumToKey(NeatConfiguration.SURVIVAL_RATE), "survival.rate");
            put(enumToKey(NeatConfiguration.ELITISM), "selector.elitism");
            put(enumToKey(NeatConfiguration.WEIGHTED_SELECTOR), "selector.roulette");
            put(enumToKey(NeatConfiguration.ELITISM_MIN_SPECIE_SIZE), "selector.elitism.min.specie.size");
            put(enumToKey(NeatConfiguration.CHROM_COMPAT_EXCESS_COEFF), "chrom.compat.excess.coeff");
            put(enumToKey(NeatConfiguration.CHROM_COMPAT_DISJOINT_COEFF), "chrom.compat.disjoint.coeff");
            put(enumToKey(NeatConfiguration.CHROM_COMPAT_COMMON_COEFF), "chrom.compat.common.coeff");
            put(enumToKey(NeatConfiguration.SPECIATION_THRESHOLD), "speciation.threshold");
            put(enumToKey(NeuralNetworkConfiguration.WEIGHT_MAX), "weight.max");
            put(enumToKey(NeuralNetworkConfiguration.WEIGHT_MIN), "weight.min");
            put(enumToKey(NeuralNetworkConfiguration.HIDDEN_ACTIVATION_FUNCTION), "initial.topology.activation");
            put(enumToKey(NeuralNetworkConfiguration.INPUT_ACTIVATION_FUNCTION), "initial.topology.activation.input");
            put(enumToKey(NeuralNetworkConfiguration.OUTPUT_ACTIVATION_FUNCTION), "initial.topology.activation.output");
            put(enumToKey(NeuralNetworkConfiguration.INPUT_LAYER_SIZE), "stimulus.size");
            put(enumToKey(NeuralNetworkConfiguration.OUTPUT_LAYER_SIZE), "response.size");
            put(enumToKey(NeatConfiguration.ADD_CONNECTION_MUTATION_RATE), "add.connection.mutation.rate");
            put(enumToKey(NeatConfiguration.ADD_NEURON_MUTATION_RATE), "add.neuron.mutation.rate");
            put(enumToKey(NeatConfiguration.REMOVE_CONNECTION_MUTATION_RATE), "remove.connection.mutation.rate");
            put(enumToKey(NeatConfiguration.REMOVE_CONNECTION_MAX_WEIGHT), "remove.connection.max.weight");
            put(enumToKey(NeatConfiguration.PRUNE_MUTATION_RATE), "prune.mutation.rate");
            put(enumToKey(NeatConfiguration.WEIGHT_MUTATION_RATE), "weight.mutation.rate");
            put(enumToKey(NeatConfiguration.WEIGHT_MUTATION_DEVIATION), "weight.mutation.std.dev");
            put(enumToKey(NeatConfiguration.RECURRENT_ALLOWED), "recurrent");
        }
    };

    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<>() {
        @Serial
        private static final long serialVersionUID = 1194318741990855812L;

        {
            put("initial.topology.fully.connected", true);
            put("initial.topology.num.hidden.neurons", 0);
            put("persistence.class", "com.anji.persistence.FilePersistence");
            put("persistence.base.dir", "temp/");
            put("persist.all", false);
            put("persist.champions", true);
            put("persist.last", true);
            put("id.file", "temp/id.xml");
            put("neat.id.file", "temp/neatid.xml");
            put("presentation.dir", "temp/");
        }
    };

    private static final Map<String, String> VALUE_MAPPING = new HashMap<>() {
        @Serial
        private static final long serialVersionUID = -4175569077115095508L;

        {
            put(ActivationFunction.IDENTITY.name(), "linear");
            put(ActivationFunction.RELU.name(), "linear");
            put(ActivationFunction.TANH.name(), "tanh");
            put(ActivationFunction.SIGMOID.name(), "sigmoid");
            put(ActivationFunction.LINEAR_UNTIL_SATURATION.name(), "clamped-linear");
        }
    };

    private ConfigurationPropertyMapper() {
    }

    public static Properties mapConfigurationToProperties(ConfigurationHandler<OptimizationConfiguration> configuration) {
        Properties properties = new Properties();

        for (OptimizationConfiguration key : configuration.getConfigurationValues().keySet()) {
            String keyString = enumToKey(key);
            if (KEY_MAPPINGS.containsKey(keyString)) {
                properties.setProperty(KEY_MAPPINGS.get(keyString), configuration.getValue(key, Object.class).toString());
            }
        }

        addNeatConfigurations(properties, configuration);
        addNeuralNetworkConfiguration(properties, configuration);
        properties.putAll(DEFAULT_VALUES);

        return properties;
    }

    private static void addNeatConfigurations(Properties properties, ConfigurationHandler<OptimizationConfiguration> configuration) {
        ConfigurationHandler<NeatConfiguration> neatConfiguration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, NeatConfiguration.class, OptimizationConfiguration.IMPLEMENTATION_CONFIGURATION);

        for (NeatConfiguration key : neatConfiguration.getConfigurationValues().keySet()) {
            String keyString = enumToKey(key);
            if (KEY_MAPPINGS.containsKey(keyString)) {
                properties.setProperty(KEY_MAPPINGS.get(keyString), neatConfiguration.getValue(key, Object.class).toString());
            }
        }
    }

    private static void addNeuralNetworkConfiguration(Properties properties, ConfigurationHandler<OptimizationConfiguration> configuration) {
        ConfigurationHandler<NeuralNetworkConfiguration> nnConfiguration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, NeuralNetworkConfiguration.class, OptimizationConfiguration.NEURAL_NETWORK_CONFIGURATION);

        for (NeuralNetworkConfiguration key : nnConfiguration.getConfigurationValues().keySet()) {
            String keyString = enumToKey(key);

            if (KEY_MAPPINGS.containsKey(keyString)) {
                String value = nnConfiguration.getValue(key, Object.class).toString();

                if (key == NeuralNetworkConfiguration.INPUT_LAYER_SIZE) {
                    // bias is input neuron for neat
                    value = Integer.toString(Integer.parseInt(value) + 1);
                }

                value = VALUE_MAPPING.getOrDefault(value, value);
                properties.setProperty(KEY_MAPPINGS.get(keyString), value);
            }
        }
    }


    private static String enumToKey(Enum<?> key) {
        return key.getClass().getSimpleName() + "." + key.name();
    }

}
