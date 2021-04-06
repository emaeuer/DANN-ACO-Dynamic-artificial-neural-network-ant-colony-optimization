package de.emaeuer.persistence;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.EmbeddedConfiguration;
import de.emaeuer.state.StateHandler;
import de.emaeuer.state.value.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JsonUtil {

    private JsonUtil() {}

    public static Object stateToJson(StateHandler<?> state, Set<Enum<?>> keysToExcludeFromRun) {
        JSONObject representation = new JSONObject();

        state.getCurrentState()
                .entrySet()
                .stream()
                .filter(e -> !keysToExcludeFromRun.contains(e.getKey()))
                .forEach(e -> representation.put(e.getKey().getKeyName(), JsonUtil.stateValueToJson(e.getValue(), keysToExcludeFromRun)));

        return representation;
    }

    private static Object stateValueToJson(AbstractStateValue<?, ?> value, Set<Enum<?>> keysToExcludeFromRun) {
        if (value == null || value.getValue() == null) {
            return null;
        }

        Object representation = null;

        if (value instanceof NumberStateValue numberValue) {
            representation = numberValue.getValue();
        } else if (value instanceof MapOfStateValue mapValue) {
            representation = mapStateValueToJson(mapValue);
        } else if (value instanceof DataSeriesStateValue seriesValue) {
            representation = seriesValueToJson(seriesValue);
        } else if (value instanceof EmbeddedState embeddedState) {
            representation = stateToJson(embeddedState.getValue(), keysToExcludeFromRun);
        }
        return representation;
    }

    private static Object seriesValueToJson(DataSeriesStateValue seriesValue) {
        JSONObject representation = new JSONObject();

        seriesValue.getValue()
                .forEach((k,v) -> representation.put(k, dataPointsToJson(v)));

        return representation;
    }

    private static Object dataPointsToJson(List<Double[]> dataPoints) {
        JSONArray pointsArray = new JSONArray();

        dataPoints.stream()
                .map(JSONArray::new)
                .forEach(pointsArray::put);

        return pointsArray;
    }

    private static Object mapStateValueToJson(MapOfStateValue mapValue) {
        JSONObject representation = new JSONObject();

        // pass empty set as keys to exclude because embedded states shouldn't be in a map
        mapValue.getValue()
                .forEach((k,v) -> representation.put(k, stateValueToJson(v, Collections.emptySet())));

        return representation;
    }

    public static Object configurationToJSON(ConfigurationHandler<?> configuration) {
        JSONObject configRoot = new JSONObject();

        configuration.getConfigurationValues()
                .forEach((k,v) -> configRoot.put(k.getKeyName(), configValueToJson(v)));

        return configRoot;
    }

    private static Object configValueToJson(AbstractConfigurationValue<?> value) {
        if (value instanceof EmbeddedConfiguration<?> embeddedConfig) {
            return configurationToJSON(embeddedConfig.getValue());
        } else {
            return value.getStringRepresentation();
        }
    }

}
