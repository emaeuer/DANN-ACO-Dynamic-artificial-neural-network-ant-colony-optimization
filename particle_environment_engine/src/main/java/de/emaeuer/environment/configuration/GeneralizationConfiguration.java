package de.emaeuer.environment.configuration;

import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.environment.bird.configuration.FlappyBirdGeneralizationConfiguration;

import java.util.List;

public interface GeneralizationConfiguration<T extends Enum<T> & DefaultConfiguration<T>> {
    // empty just marker interface
}
