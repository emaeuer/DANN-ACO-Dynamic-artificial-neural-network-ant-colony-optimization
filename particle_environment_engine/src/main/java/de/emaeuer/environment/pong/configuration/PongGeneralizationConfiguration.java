package de.emaeuer.environment.pong.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.environment.bird.configuration.FlappyBirdGeneralizationConfiguration;
import de.emaeuer.environment.configuration.GeneralizationConfiguration;

public enum PongGeneralizationConfiguration implements DefaultConfiguration<PongGeneralizationConfiguration>, GeneralizationConfiguration<PongGeneralizationConfiguration> {
    ;

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    PongGeneralizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) defaultValue.getClass();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public AbstractConfigurationValue<?> getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Class<?> getValueType() {
        return this.type;
    }

    @Override
    public boolean refreshNecessary() {
        return false;
    }

    @Override
    public String getKeyName() {
        return name();
    }
}
