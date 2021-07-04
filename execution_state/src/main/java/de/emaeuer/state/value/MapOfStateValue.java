package de.emaeuer.state.value;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MapOfStateValue extends AbstractStateValue<Entry<String, AbstractStateValue<?, ?>>, Map<String, AbstractStateValue<?, ?>>> {

    private final Map<String, AbstractStateValue<?,?>> value = new ConcurrentHashMap<>();

    @Override
    public Class<? extends Entry<String, AbstractStateValue<?, ?>>> getExpectedInputType() {
        Class<?> type = Entry.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Entry<String, AbstractStateValue<?, ?>>>) type;
    }

    @Override
    public Class<? extends Map<String, AbstractStateValue<?, ?>>> getOutputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<String, AbstractStateValue<?, ?>>>) type;
    }

    @Override
    protected String handleNewValue(Entry<String, AbstractStateValue<?, ?>> value) {
        if (value instanceof EmbeddedState || value instanceof MapOfStateValue) {
            throw new UnsupportedOperationException("MapOfStateValue doesn't support selection of embedded states or further selections");
        }

        this.value.put(value.getKey(), value.getValue());
        return String.format("%s:%s", value.getKey(), value.getValue().getExportValue());
    }

    @Override
    public Map<String, AbstractStateValue<?,?>> getValueImpl() {
        return this.value;
    }

    @Override
    public String getExportValue() {
        return this.value.entrySet()
                .stream()
                .map(e -> String.format("[%s:%s]", e.getKey(), e.getValue().getExportValue()))
                .collect(Collectors.joining(","));
    }
}
