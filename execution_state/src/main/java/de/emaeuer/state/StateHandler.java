package de.emaeuer.state;

import de.emaeuer.persistence.BackgroundFileWriter;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.StateValueFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class StateHandler<T extends Enum<T> & StateParameter<T>> implements AutoCloseable {

    private final static Logger LOG = LogManager.getLogger(StateHandler.class);

    private final Class<T> parameterClass;

    private String stateName;

    private final Map<T, AbstractStateValue<?, ?>> currentState;

    private final Lock lock = new ReentrantLock(true);

    private final StateHandler<?> parent;
    private final String parentPrefix;

    private final BackgroundFileWriter writer;

    public StateHandler(Class<T> parameterClass) {
        this(parameterClass, null, null);
    }

    public StateHandler(Class<T> parameterClass, BackgroundFileWriter writer) {
        this(parameterClass, null, writer);
    }

    public StateHandler(Class<T> parameterClass, StateHandler<?> parent) {
        this(parameterClass, parent, parent == null ? null : parent.writer);
    }

    public StateHandler(Class<T> parameterClass, StateHandler<?> parent, BackgroundFileWriter writer) {
        this.parameterClass = parameterClass;

        this.stateName = parameterClass.getName() + "_" +
                new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());

        this.parent = parent;
        this.parentPrefix = this.parent == null ? null : this.parent.getPrefix();
        this.writer = writer;

        this.currentState = createInitializedState();
    }

    private Map<T, AbstractStateValue<?, ?>> createInitializedState() {
        EnumMap<T, AbstractStateValue<?, ?>> state = new EnumMap<>(this.parameterClass);

        for (T key : this.parameterClass.getEnumConstants()) {
            AbstractStateValue<?, ?> value = StateValueFactory.createValueForClass(key.getExpectedValueType());
            state.put(key, value);
        }

        return state;
    }

    public void resetValue(T key) {
        // replace old value by default value
        this.currentState.put(key, StateValueFactory.createValueForClass(key.getExpectedValueType()));
    }

    public void addNewValue(T key, Object newValue) {
        AbstractStateValue<?, ?> value = this.currentState.get(key);

        try {
            String result = value.newValue(newValue);
            persistValue(key, result);
        } catch (IllegalArgumentException e) {
            LOG.warn("An exception occurred while adding a new value, ignoring this value", e);
        }
    }

    public <S> S getValue(T key, Class<S> expectedType) {
        AbstractStateValue<?, ?> value = this.currentState.get(key);

        if (!expectedType.isAssignableFrom(value.getOutputType())) {
            throw new IllegalArgumentException(String.format("Can't cast %s to %s", value.getOutputType().getSimpleName(), expectedType.getSimpleName()));
        }

        //noinspection unchecked no safe way to cast generic but was checked
        return (S) value.getValue();
    }

    public void export(T key) {
        if (key == null) {
            return;
        }

        AbstractStateValue<?, ?> value = this.currentState.get(key);

        if (value != null && writer != null) {
            writer.writeLine(String.format("%s.%s = %s", getPrefix(), key.getKeyName() , value.getExportValue()));
        }
    }

    private void persistValue(T key, String value) {
        if (writer != null && value != null && key.export()) {
            writer.writeLine(String.format("%s.%s = %s", getPrefix(), key.getKeyName() , value));
        }
    }

    private String getPrefix() {
        if (this.parentPrefix == null) {
            return getName();
        } else {
            return String.format("%s.%s", parent.getPrefix(), getName());
        }
    }

    public Map<T, AbstractStateValue<?, ?>> getCurrentState() {
        return currentState;
    }

    public String getName() {
        return this.stateName;
    }

    public void setName(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public void close() throws Exception {
        this.writer.close();
    }

    public void execute(Consumer<StateHandler<T>> consumer) {
        try {
            this.lock.lock();
            consumer.accept(this);
        } finally {
            this.lock.unlock();
        }
    }

}