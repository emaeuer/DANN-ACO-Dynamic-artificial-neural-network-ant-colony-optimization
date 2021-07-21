package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.bird.elements.Pipe;

import java.util.List;

public class PipeShape implements Shape<Pipe> {

    private static final int PIPE_LENGTH = 1545;

    @Override
    public List<ShapeEntity> getShapesForElement(Pipe element) {
        ShapeEntity topHalf = createShapeForTop(element);
        ShapeEntity bottomHalf = createShapeForBottom(element);

        return List.of(topHalf, bottomHalf);
    }

    private ShapeEntity createShapeForTop(Pipe element) {
        double gapPosition = element.getGapPosition();

        double[] yCoords = new double[] {gapPosition - PIPE_LENGTH, PIPE_LENGTH};

        ImageShape imageShape = new ImageShape(getClass().getResource("/flappy_bird/pipe_down.png").toExternalForm());

        return new ShapeEntity(Shapes.IMAGE, imageShape, getXCoordinates(element), yCoords, "rgba(0,209,42,1.0)");
    }

    private ShapeEntity createShapeForBottom(Pipe element) {
        double upperY = element.getGapPosition() + element.getGapSize();

        double[] yCoords = new double[] {upperY, PIPE_LENGTH};

        ImageShape imageShape = new ImageShape(getClass().getResource("/flappy_bird/pipe_up.png").toExternalForm());

        return new ShapeEntity(Shapes.IMAGE, imageShape, getXCoordinates(element), yCoords, "rgba(0,209,42,1.0)");
    }

    private double[] getXCoordinates(Pipe element) {
        return new double[] {element.getPosition().getX(), element.getWidth()};
    }
}
