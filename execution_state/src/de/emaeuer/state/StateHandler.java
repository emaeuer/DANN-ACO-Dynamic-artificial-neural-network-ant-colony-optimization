package de.emaeuer.state;

import de.emaeuer.persistence.Persistable;
import de.emaeuer.state.value.AbstractStateValue;
import de.emaeuer.state.value.StateValueFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serial;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

public class StateHandler<T extends Enum<T> & StateParameter<T>> implements Persistable<StateHandler<?>> {

    private final static Logger LOG = LogManager.getLogger(StateHandler.class);

    @Serial
    private static final long serialVersionUID = 8227804954777422372L;

    private static final String STATE_COLLECTION = "state";

    private final Class<T> parameterClass;

    private String stateName;

    private final Map<T, AbstractStateValue<?, ?>> currentState;

    public StateHandler(Class<T> parameterClass) {
        this.parameterClass = parameterClass;

        this.stateName = parameterClass.getName() + "_" +
                new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());

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

    public void addNewValue(T key, Object newValue) {
        AbstractStateValue<?, ?> value = this.currentState.get(key);

        try {
            value.newValue(newValue);
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

    public Map<T, AbstractStateValue<?, ?>> getCurrentState() {
        return currentState;
    }

    @Override
    public String getName() {
        return this.stateName;
    }

    public void setName(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getClassName() {
        return this.parameterClass.getSimpleName();
    }

    @Override
    public void applyOther(StateHandler<?> other) {
        if (!this.parameterClass.equals(other.parameterClass)) {
            throw new IllegalArgumentException("Failed to apply state because of different key types");
        }

        //noinspection unchecked no safe way to cast generic but was checked
        this.currentState.putAll((Map<? extends T, ? extends AbstractStateValue<?, ?>>) other.currentState);
    }

    public Class<T> getParameterClass() {
        return parameterClass;
    }

    @Override
    public String getCollectionName() {
        return STATE_COLLECTION;
    }
}