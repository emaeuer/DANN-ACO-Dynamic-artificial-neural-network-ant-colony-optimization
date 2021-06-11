package de.emaeuer.persistence;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.EmbeddedConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.*;

public class ConfigurationIOHandler {

    private static final Logger LOG = LogManager.getLogger(ConfigurationHandler.class);

    private static final String CONFIG_TYPE = "CONFIG_TYPE";

    private ConfigurationIOHandler() {}

    public static void exportConfiguration(ConfigurationHandler<?> configuration, File file) {
        writeJSON(configToJson(configuration), file);
    }

    private static JSONObject configToJson(ConfigurationHandler<?> configuration) {
        JSONObject json = new JSONObject();
        json.put(CONFIG_TYPE, configuration.getKeyClass().getName());

        for (Map.Entry<? extends Enum<?>, AbstractConfigurationValue<?>> configValue : configuration.getConfigurationValues().entrySet()) {
            if (configValue.getValue() instanceof EmbeddedConfiguration<?> embeddedConfiguration) {
                json.put(configValue.getKey().name(), configToJson(embeddedConfiguration.getValue()));
            } else {
                json.put(configValue.getKey().name(), configValue.getValue().getStringRepresentation());
            }
        }

        return json;
    }

    private static void writeJSON(JSONObject json, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json.toString(4));
            writer.flush();
        } catch (IOException e) {
            LOG.warn("Failed to write json to file " + file.getAbsolutePath(), e);
        }
    }

    public static void importConfiguration(ConfigurationHandler<?> configuration, File file) {
        JSONObject json = loadJSON(file);

        try {
            applyJSONToConfig(json, configuration);
        } catch (JSONException e) {
            LOG.warn("Unexpected error while parsing the json file " + file.getAbsolutePath(), e);
        }
    }

    private static void applyJSONToConfig(JSONObject json, ConfigurationHandler<?> configuration) {
        if (json == null) {
            return;
        } else if (!configuration.getKeyClass().getName().equals(retrieveStringValueFromJSON(CONFIG_TYPE, json))) {
            LOG.warn("Can't load json file because the content type {} differs from the expected config type {}",
                    retrieveStringValueFromJSON(CONFIG_TYPE, json), configuration.getKeyClass().getName());
            return;
        }

        // The embedded configs are added later because the other values may trigger actions which set the correct type of the embedded configuration
        List<String> embeddedConfigKeys = new ArrayList<>();

        for (Map.Entry<? extends Enum<?>, AbstractConfigurationValue<?>> configValue : configuration.getConfigurationValues().entrySet()) {
            if (configValue.getValue() instanceof EmbeddedConfiguration<?> embeddedConfiguration) {
                embeddedConfigKeys.add(configValue.getKey().name());
            } else {
                String value = retrieveStringValueFromJSON(configValue.getKey().name(), json);
                if (value != null) {
                    configuration.setValue(configValue.getKey().name(), value);
                }
            }
        }

        for (String configKey : embeddedConfigKeys) {
            JSONObject embeddedJSON = retrieveObjectValueFromJSON(configKey, json);
            applyJSONToConfig(embeddedJSON, configuration.getValue(configKey, ConfigurationHandler.class));
        }
    }

    private static JSONObject retrieveObjectValueFromJSON(String name, JSONObject json) {
        try {
            return json.getJSONObject(name);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String retrieveStringValueFromJSON(String name, JSONObject json) {
        try {
            return json.getString(name);
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONObject loadJSON(File file) {
        try (InputStream input = new FileInputStream(file)) {
            JSONTokener tokenizer = new JSONTokener(input);
            return new JSONObject(tokenizer);
        } catch (Exception e) {
            LOG.warn("Failed to read json from file " + file.getAbsolutePath(), e);
            return null;
        }
    }

}
