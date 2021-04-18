package de.emaeuer.configuration;

import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.persistence.Persistable;

import java.io.Serial;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigurationHandler<T extends Enum<T> & DefaultConfiguration<T>> implements Persistable<ConfigurationHandler<T>> {

    @Serial
    private static final long serialVersionUID = -1995290222858485618L;

    private static final String CONFIGURATION_COLLECTION = "configuration";

    private final Class<T> keyEnum;

    private final EnumMap<T, AbstractConfigurationValue<?>> configurationValues;

    private String configurationName;

    public ConfigurationHandler(Class<T> defaultConfiguration) {
        this.keyEnum = defaultConfiguration;

        // initialize configuration values
        this.configurationValues = Arrays.stream(this.keyEnum.getEnumConstants())
                .filter(c -> c.getDefaultValue() != null) // this values have to be set manually
                .collect(Collectors.toMap(c -> c, c -> c.getDefaultValue().copy(), // copy to prevent problems with changes of the default value
                        (l, r) -> r, // duplicated keys are impossible because an enum can't have duplicated values
                        () -> new EnumMap<>(this.keyEnum)));

        // Default configuration name
        this.configurationName = defaultConfiguration.getName() + "_" +
                new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
    }

    public void setValue(String key, Object value) {
        Arrays.stream(this.keyEnum.getEnumConstants())
                .filter(k -> k.name().equals(key))
                .findFirst()
                .ifPresent(k -> setValue(k, value));
    }

    public void setValue(T key, Object value) {
        AbstractConfigurationValue<?> configValue = this.configurationValues.get(key);
        configValue.setValue(value.toString());
        setValue(key, configValue);
    }

    public void setValue(T key, AbstractConfigurationValue<?> value) {
        this.configurationValues.put(key, value);
        key.executeChangeAction(value, this);
    }

    public String getStringRepresentation(T key) {
        return this.configurationValues.get(key).getStringRepresentation();
    }

    public <S> S getValue(T key, Class<S> expectedValueType) {
        return getValue(key, expectedValueType, Collections.emptyMap());
    }

    public <S> S getValue(T key, Class<S> expectedValueType, Map<String, Double> variables) {
        var value = this.configurationValues.get(key).getValueForState(variables);

        if (expectedValueType.isInstance(value)) {
            return expectedValueType.cast(value);
        } else if (Number.class.isAssignableFrom(expectedValueType) && value instanceof Number number) {
            if (Integer.class.equals(expectedValueType)) {
                return expectedValueType.cast(number.intValue());
            } else if (Double.class.equals(expectedValueType)) {
                return expectedValueType.cast(number.doubleValue());
            }
        } else if (Boolean.class.isAssignableFrom(expectedValueType) && value instanceof Number number) {
            return expectedValueType.cast(number.doubleValue() != 0);
        }

        throw new IllegalArgumentException(String.format("Expected value type %s doesn't match actual value type %s", expectedValueType.getSimpleName(), value.getClass().getSimpleName()));
    }

    public Map<T, String> getConfigurations() {
        return this.configurationValues.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStringRepresentation()));
    }

    public EnumMap<T, AbstractConfigurationValue<?>> getConfigurationValues() {
        return this.configurationValues;
    }

    @Override
    public void applyOther(ConfigurationHandler<T> other) {
        if (!this.keyEnum.equals(other.keyEnum)) {
            throw new IllegalArgumentException("Failed to apply configuration because of different key types");
        }
        other.configurationValues.forEach(this::setValue);
    }

    @Override
    public String getCollectionName() {
        return CONFIGURATION_COLLECTION;
    }

    public Class<T> getKeyClass() {
        return keyEnum;
    }

    @Override
    public String getName() {
        return this.configurationName;
    }

    public void setName(String configurationName) {
        this.configurationName = configurationName;
    }

    @Override
    public String getClassName() {
        return this.keyEnum.getSimpleName();
    }
}
