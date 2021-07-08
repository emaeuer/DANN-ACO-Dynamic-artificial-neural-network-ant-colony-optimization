package de.emaeuer.configuration;

import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.NumericListConfigurationValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ConfigurationHelper {

    private final static Logger LOG = LogManager.getLogger(ConfigurationHelper.class);

    private ConfigurationHelper() {}

    public static <T extends Enum<T> & DefaultConfiguration<T>, S extends Enum<S> & DefaultConfiguration<S>> ConfigurationHandler<T> extractEmbeddedConfiguration(ConfigurationHandler<S> rootHandler, Class<T> embeddedType, S key) {
        //noinspection unchecked
        ConfigurationHandler<T> handler = (ConfigurationHandler<T>) rootHandler.getValue(key, ConfigurationHandler.class);

        if (handler.getKeyClass() != embeddedType) {
            String message = String.format("Type mismatch of embedded configuration (%s != %s)", handler.getKeyClass().getSimpleName(), embeddedType.getSimpleName());
            LOG.warn(message);
            throw new IllegalArgumentException(message);
        }

        return handler;
    }

    public static <T extends Enum<T> & DefaultConfiguration<T>, S extends Enum<S> & DefaultConfiguration<S>> List<Double> getNumericListValue(ConfigurationHandler<S> config, S key) {
        AbstractConfigurationValue<?> value = config.getConfigurationValues().get(key);
        if (value instanceof NumericListConfigurationValue listValue) {
            return listValue.getValueForState(null);
        }

        String message = String.format("Can't extract list of numbers from configuration value of type %s", value.getClass().getSimpleName());
        LOG.warn(message);
        throw new IllegalArgumentException(message);
    }
}
