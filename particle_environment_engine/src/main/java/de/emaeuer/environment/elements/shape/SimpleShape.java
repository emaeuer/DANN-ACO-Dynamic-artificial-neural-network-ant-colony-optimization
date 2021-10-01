package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.elements.AbstractElement;

import java.util.Collections;
import java.util.List;

public class SimpleShape implements Shape<AbstractElement> {

    private final Shapes shape;

    public SimpleShape(Shapes shape) {
        this.shape = shape;
    }

    @Override
    public List<ShapeEntity> getShapesForElement(AbstractElement element) {
        double[] xCoords = createXCoords(element);
        double[] yCoords = createYCoords(element);

        ShapeEntity entity = new ShapeEntity(this.shape, this, xCoords, yCoords);

        return Collections.singletonList(entity);
    }

    private double[] createXCoords(AbstractElement element) {
        double width = element.getSize().getX();
        double x = element.getPosition().getX();

        return switch (this.shape) {
            case CIRCLE -> new double[] {x - width / 2, width};
            case LINE -> new double[] {x, x + width};
            case SQUARE, POLYGON -> new double[] {x - width / 2, x + width / 2, x + width / 2, x - width / 2};
            case IMAGE -> null;
        };
    }

    private double[] createYCoords(AbstractElement element) {
        double height = element.getSize().getY();
        double y = element.getPosition().getY();

        return switch (this.shape) {
            case CIRCLE -> new double[] {y - height / 2, height};
            case LINE -> new double[] {y, y + height};
            case SQUARE, POLYGON -> new double[] {y - height / 2, y - height / 2, y + height / 2, y + height / 2};
            case IMAGE -> null;
        };
    }

}
