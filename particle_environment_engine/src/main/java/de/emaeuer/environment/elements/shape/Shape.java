package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.elements.AbstractElement;

import java.util.List;

public interface Shape<T extends AbstractElement> {

    List<ShapeEntity> getShapesForElement(T element);

}
