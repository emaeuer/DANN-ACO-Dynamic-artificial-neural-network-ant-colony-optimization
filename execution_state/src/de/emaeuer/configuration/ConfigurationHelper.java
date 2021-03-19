package de.emaeuer.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

}
