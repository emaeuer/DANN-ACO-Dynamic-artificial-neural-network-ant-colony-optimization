package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.bird.elements.Pipe;

import java.util.List;

public class PipeShape implements Shape<Pipe> {

    @Override
    public List<ShapeEntity> getShapesForElement(Pipe element) {
        ShapeEntity topHalf = createShapeForTop(element);
        ShapeEntity bottomHalf = createShapeForBottom(element);

        return List.of(topHalf, bottomHalf);
    }

    private ShapeEntity createShapeForTop(Pipe element) {
        double gapPosition = element.getGapPosition();

        double[] yCoords = new double[] {0, gapPosition, gapPosition, 0};

        return new ShapeEntity(BasicShape.SQUARE, getXCoordinates(element), yCoords);
    }

    private ShapeEntity createShapeForBottom(Pipe element) {
        double upperY = element.getGapPosition() + element.getGapSize();
        double lowerY = element.getSize().getY() + upperY;

        double[] yCoords = new double[] {upperY, lowerY, lowerY, upperY};

        return new ShapeEntity(BasicShape.SQUARE, getXCoordinates(element), yCoords);
    }

    private double[] getXCoordinates(Pipe element) {
        double xPosition = element.getPosition().getX();
        double width = element.getWidth();

        return new double[] {xPosition, xPosition, xPosition + width, xPosition + width};
    }
}
