package de.emaeuer.persistence;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SingletonDataExporter {

    private static final Logger LOG = LogManager.getLogger(SingletonDataExporter.class);

    private static String fileName = "data.json";


    // concurrent set
    private static final Set<Enum<?>> KEYS_TO_EXCLUDE_FROM_RUN = ConcurrentHashMap.newKeySet();

    private static JSONObject root = new JSONObject();

    private SingletonDataExporter() {}

    public synchronized static void reset() {
        root = new JSONObject();
        fileName = "execution_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".json";
    }

    public synchronized static void exportConfiguration(ConfigurationHandler<?> configuration) {
        root.put("configuration", JsonUtil.configurationToJSON(configuration));
    }

    public synchronized static void exportRunSummary(StateHandler<?> state) {
        if (root.isNull("runs")) {
            root.put("runs", new JSONArray());
        }

        JSONArray runArray = root.getJSONArray("runs");

        runArray.put(JsonUtil.stateToJson(state, KEYS_TO_EXCLUDE_FROM_RUN));
    }

    public static void finishAndExport() {
        File file = new File("temp", fileName);
        try (FileWriter writer = new FileWriter(file)) {
            root.write(writer, 4, 0);
        } catch (IOException e) {
            LOG.warn("Failed to export json file for finished execution", e);
        }
    }

    public static void addValueToExcludeFromRun(Enum<?>... key) {
        KEYS_TO_EXCLUDE_FROM_RUN.addAll(Arrays.asList(key));
    }

}
