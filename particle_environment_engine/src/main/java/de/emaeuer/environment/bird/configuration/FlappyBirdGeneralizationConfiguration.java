package de.emaeuer.environment.bird.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;
import de.emaeuer.configuration.value.NumericListConfigurationValue;
import de.emaeuer.environment.configuration.GeneralizationConfiguration;

import java.util.Arrays;
import java.util.List;

public enum FlappyBirdGeneralizationConfiguration implements DefaultConfiguration<FlappyBirdGeneralizationConfiguration>, GeneralizationConfiguration<FlappyBirdGeneralizationConfiguration> {
    NUMBER_OF_SEEDS("Number of random seeds to try", new IntegerConfigurationValue(10, 1, 100)),
    BIRD_START_HEIGHTS("Start height of the birds", new NumericListConfigurationValue("0.1, 0.25, 0.5, 0.75, 0.9")),
    GAP_SIZES("Gap sizes relative to the defined value", new NumericListConfigurationValue("0.7, 1, 1.3")),
    PIPE_DISTANCES("Pipe distance relative to the defined value", new NumericListConfigurationValue("0.5, 1, 1.5")),
    PIPE_WIDTHS("Pipe width relative to the defined value", new NumericListConfigurationValue("0.5, 1, 1.5"));

    private final String name;
    private final AbstractConfigurationValue<?> defaultValue;
    private final Class<? extends AbstractConfigurationValue<?>> type;

    FlappyBirdGeneralizationConfiguration(String name, AbstractConfigurationValue<?> defaultValue) {
        this.defaultValue = defaultValue;
        //noinspection unchecked no safe way to cast generic
        this.type = (Class<? extends AbstractConfigurationValue<?>>) defaultValue.getClass();
        this.name = name;
    }

    public static List<FlappyBirdGeneralizationConfiguration> getKeysForGeneralization() {
        return Arrays.asList(GAP_SIZES, PIPE_DISTANCES, PIPE_WIDTHS);
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
