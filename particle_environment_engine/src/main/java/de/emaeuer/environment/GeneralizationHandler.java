package de.emaeuer.environment;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.environment.configuration.GeneralizationConfiguration;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public class GeneralizationHandler<T extends Enum<T> & DefaultConfiguration<T> & GeneralizationConfiguration<T>> {

    private final ConfigurationHandler<T> config;

    private final EnumMap<T, Integer> indices;

    private final List<T> generalizationKeys;

    private boolean reachedEnd = false;

    private final int numberOfIterations;

    public GeneralizationHandler(ConfigurationHandler<T> config, List<T> generalizationKeys) {
        this.config = config;
        this.generalizationKeys = generalizationKeys;

        indices = new EnumMap<>(config.getKeyClass());
        generalizationKeys.forEach(k -> indices.put(k, 0));

        this.numberOfIterations = calculateNumberOfIterations();
    }

    private int calculateNumberOfIterations() {
        if (this.config == null) {
            return 0;
        }

        int result = 1;

        for (T key : this.generalizationKeys) {
            result *= this.config.getValue(key, List.class).size();
        }

        return result;
    }

    public void next() {
        if (reachedEnd()) {
            return;
        }

        for (T key : this.generalizationKeys) {
            int index = this.indices.compute(key, (k, v) -> Objects.requireNonNullElse(v, 0) + 1);
            if (index < this.config.getValue(key, List.class).size()) {
                break;
            } else {
                this.indices.put(key, 0);
                if (key == this.generalizationKeys.get(this.generalizationKeys.size() - 1)) {
                    this.reachedEnd = true;
                }
            }
        }
    }

    public double getNextValue(T key) {
        return ConfigurationHelper.getNumericListValue(this.config, key).get(this.indices.get(key));
    }

    public boolean reachedEnd() {
        return this.reachedEnd;
    }

    public int getNumberOfGeneralizationIterations() {
        return this.numberOfIterations;
    }

    protected ConfigurationHandler<T> getConfig() {
        return config;
    }
}
