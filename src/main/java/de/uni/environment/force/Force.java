package de.uni.environment.force;

import de.uni.environment.elements.AbstractElement;

import java.util.function.Consumer;

/**
 * Marker interface for type safety of the generic
 */
public interface Force extends Consumer<AbstractElement> {

}
