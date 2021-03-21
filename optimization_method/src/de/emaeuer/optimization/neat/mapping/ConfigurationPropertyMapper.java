package de.emaeuer.optimization.neat.mapping;

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
            put(enumToKey(OptimizationConfiguration.OPTIMIZATION_MAX_NUMBER_OF_EVALUATIONS), "num.generations");
            put(enumToKey(NeatConfiguration.POPULATION_SIZE), "popul.size");
            put(enumToKey(NeatConfiguration.TOPOLOGY_MUTATION_CLASSIC), "topology.mutation.classic");
//            put(, "add.connection.mutation.rate");
//            put(, "remove.connection.mutation.rate");
//            put(, "remove.connection.max.weight");
//            put(, "add.neuron.mutation.rate");
//            put(, "prune.mutation.rate");
//            put(, "weight.mutation.rate");
//            put(, "weight.mutation.std.dev");
            put(enumToKey(NeatConfiguration.WEIGHT_MAX), "weight.max");
            put(enumToKey(NeatConfiguration.WEIGHT_MIN), "weight.min");
            put(enumToKey(NeatConfiguration.SURVIVAL_RATE), "survival.rate");
            put(enumToKey(NeatConfiguration.ELITISM), "selector.elitism");
            put(enumToKey(NeatConfiguration.WEIGHTED_SELECTOR), "selector.roulette");
            put(enumToKey(NeatConfiguration.ELITISM_MIN_SPECIE_SIZE), "selector.elitism.min.specie.size");
            put(enumToKey(NeatConfiguration.CHROM_COMPAT_EXCESS_COEFF), "chrom.compat.excess.coeff");
            put(enumToKey(NeatConfiguration.CHROM_COMPAT_DISJOINT_COEFF), "chrom.compat.disjoint.coeff");
            put(enumToKey(NeatConfiguration.CHROM_COMPAT_COMMON_COEFF), "chrom.compat.common.coeff");
            put(enumToKey(NeatConfiguration.SPECIATION_THRESHOLD), "speciation.threshold");
            put(enumToKey(NeatConfiguration.INITIAL_TOPOLOGY_FULLY_CONNECTED), "initial.topology.fully.connected");
            put(enumToKey(NeatConfiguration.INITIAL_TOPOLOGY_NUM_HIDDEN_NEURONS), "initial.topology.num.hidden.neurons");
            put(enumToKey(NeatConfiguration.INITIAL_TOPOLOGY_ACTIVATION), "initial.topology.activation");
            put(enumToKey(NeatConfiguration.INITIAL_TOPOLOGY_ACTIVATION_INPUT), "initial.topology.activation.input");
            put(enumToKey(NeatConfiguration.INITIAL_TOPOLOGY_ACTIVATION_OUTPUT), "initial.topology.activation.output");
            put(enumToKey(OptimizationConfiguration.NN_INPUT_LAYER_SIZE), "stimulus.size");
            put(enumToKey(OptimizationConfiguration.NN_OUTPUT_LAYER_SIZE), "response.size");
        }
    };

    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<>() {
        @Serial
        private static final long serialVersionUID = 1194318741990855812L;

        {
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
        properties.putAll(DEFAULT_VALUES);

        return properties;
    }

    private static void addNeatConfigurations(Properties properties, ConfigurationHandler<OptimizationConfiguration> configuration) {
        ConfigurationHandler<NeatConfiguration> neatConfiguration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, NeatConfiguration.class, OptimizationConfiguration.OPTIMIZATION_CONFIGURATION);

        for (NeatConfiguration key : neatConfiguration.getConfigurationValues().keySet()) {
            String keyString = enumToKey(key);
            if (KEY_MAPPINGS.containsKey(keyString)) {
                properties.setProperty(KEY_MAPPINGS.get(keyString), neatConfiguration.getValue(key, Object.class).toString());
            }
        }
    }


    private static String enumToKey(Enum<?> key) {
        return key.getClass().getSimpleName() + "." + key.name();
    }

}
