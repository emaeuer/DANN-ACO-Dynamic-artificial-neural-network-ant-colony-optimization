package de.emaeuer.persistence;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.AbstractStateValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SingletonDataExporter {

    private static final Logger LOG = LogManager.getLogger(SingletonDataExporter.class);

    private static String fileName = "data.json";

    private static JSONObject root = new JSONObject();
    private static JSONObject runRoot = new JSONObject();

    private SingletonDataExporter() {}

    public synchronized static void reset() {
        root = new JSONObject();
        runRoot = new JSONObject();
        fileName = "execution_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".json";
    }

    public synchronized static void exportConfiguration(String configurationName, ConfigurationHandler<?> configuration) {
        root.put(configurationName, JsonUtil.configurationToJSON(configuration));
    }

    public synchronized static void addRunData(Enum<?> key, StateHandler<?> handler) {
        runRoot.put(key.name(), JsonUtil.stateValueToJson(handler.getCurrentState().getOrDefault(key, null)));
    }

    public synchronized static void addRunData(String key, Object value, boolean isArray) {
        if (isArray) {
            if (runRoot.isNull(key)) {
                runRoot.put(key, new JSONArray());
            }

            JSONArray array = runRoot.getJSONArray(key);
            array.put(value);
        } else {
            runRoot.put(key, value);
        }
    }

    public synchronized static void addData(Enum<?> key, StateHandler<?> handler) {
        root.put(key.name(), JsonUtil.stateValueToJson(handler.getCurrentState().getOrDefault(key, null)));
    }

    public synchronized static void finishRun() {
        if (root.isNull("runs")) {
            root.put("runs", new JSONArray());
        }

        JSONArray runArray = root.getJSONArray("runs");

        runArray.put(runRoot);
        runRoot = new JSONObject();
    }

    public static void finishAndExport() {
        File file = new File("temp", fileName);
        try (FileWriter writer = new FileWriter(file)) {
            root.write(writer, 4, 0);
        } catch (IOException e) {
            LOG.warn("Failed to export json file for finished execution", e);
        }
    }
}
