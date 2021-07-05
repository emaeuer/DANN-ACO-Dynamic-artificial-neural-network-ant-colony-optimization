package de.emaeuer.configuration;

import de.emaeuer.configuration.value.EmbeddedConfiguration;
import de.emaeuer.persistence.BackgroundFileWriter;
import de.emaeuer.configuration.value.AbstractConfigurationValue;

import java.util.Map;
import java.util.Objects;

public class ConfigurationUtil {

    private ConfigurationUtil() {
    }

    public static void printConfiguration(ConfigurationHandler<?> config, BackgroundFileWriter writer) {
        printConfiguration(config, writer, null);
    }

    private static void printConfiguration(ConfigurationHandler<?> config, BackgroundFileWriter writer, String prefix) {
        if (config == null) {
            return;
        }

        if (prefix == null) {
            prefix = "";
        } else {
            prefix = prefix + ".";
        }

        Map<? extends DefaultConfiguration<?>, AbstractConfigurationValue<?>> values = config.getConfigurationValues();

        for (Map.Entry<? extends DefaultConfiguration<?>, AbstractConfigurationValue<?>> setting : values.entrySet()) {
            if (setting.getValue() instanceof EmbeddedConfiguration<?> embeddedConfig) {
                printConfiguration(embeddedConfig.getValue(), writer, prefix + config.getName());
            } else {
                String value = setting.getValue() == null ? null : setting.getValue().getStringRepresentation();
                String line = String.format("%s%s.%s=%s", prefix, config.getName(), setting.getKey().getKeyName(), value);
                writer.writeLine(line);
            }
        }
    }

}
