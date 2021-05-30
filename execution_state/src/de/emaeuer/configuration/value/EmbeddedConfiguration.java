package de.emaeuer.configuration.value;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;

import java.io.Serial;
import java.util.Map;

public class EmbeddedConfiguration<S extends Enum<S> & DefaultConfiguration<S>> extends AbstractConfigurationValue<ConfigurationHandler<S>> {

    @Serial
    private static final long serialVersionUID = -7518958714698536012L;

    private ConfigurationHandler<S> value;

    public EmbeddedConfiguration(ConfigurationHandler<S> value) {
        super(null);
        this.value = value;
    }

    @Override
    public void setValue(String value) {
        if (this.value != null && !this.value.getName().equals(value)) {
            this.value = new ConfigurationHandler<>(this.value.getKeyClass());
            this.value.setName(value);
        }
    }

    @Override
    public String getStringRepresentation() {
        return this.value.getName();
    }

    public ConfigurationHandler<S> getValue() {
        return this.value;
    }

    @Override
    public ConfigurationHandler<S> getValueForState(Map<String, Double> variables) {
        return this.value;
    }

    @Override
    public AbstractConfigurationValue<ConfigurationHandler<S>> copy() {
        ConfigurationHandler<S> handlerCopy = new ConfigurationHandler<>(this.value.getKeyClass());
        handlerCopy.setName(this.value.getName());
        return new EmbeddedConfiguration<>(handlerCopy);
    }
}
