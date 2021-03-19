package de.emaeuer.environment.elements.shape;

public record ShapeEntity(BasicShape shape, double[] xCoords, double[] yCoords) {

    public ShapeEntity {
        validateInput(shape, xCoords, yCoords);
    }

    private void validateInput(BasicShape shape, double[] xCoords, double[] yCoords) {
        if (xCoords.length != yCoords.length) {
            throw new IllegalArgumentException("The number of x coordinates doesn't match the number of y coordinates");
        }

        if (xCoords.length != 2 && this.shape == BasicShape.CIRCLE) {
            throw new IllegalArgumentException("To define a circle two coordinates are needed (center and one on the circle)");
        } else if (xCoords.length != 4 && this.shape == BasicShape.SQUARE) {
            throw new IllegalArgumentException("To define a rectangle four coordinates are needed (4 vertices)");
        }
    }

}
