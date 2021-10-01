package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.DefaultConfiguration;

public interface GeneralizationConfiguration<T extends Enum<T> & DefaultConfiguration<T>> {
    // empty just marker interface
}
