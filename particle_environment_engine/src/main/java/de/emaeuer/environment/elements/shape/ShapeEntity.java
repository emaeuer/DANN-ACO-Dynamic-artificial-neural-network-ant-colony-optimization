package de.emaeuer.environment.elements.shape;

public record ShapeEntity(Shapes shapeType, Shape<?> shape, double[] xCoords, double[] yCoords, String color) {

    private static final String DEFAULT_COLOR = "rgba(0,0,0,1.0)";

    public ShapeEntity(Shapes shapeType, Shape<?> shape, double[] xCoords, double[] yCoords, String color) {
        this.shapeType = shapeType;
        this.shape = shape;
        this.xCoords = xCoords;
        this.yCoords = yCoords;
        this.color = color;
        validateInput();
    }

    public ShapeEntity(Shapes shapeType, Shape<?> shape, double[] xCoords, double[] yCoords) {
        this(shapeType, shape, xCoords, yCoords, DEFAULT_COLOR);
    }

    private void validateInput() {
        if (this.xCoords.length != this.yCoords.length) {
            throw new IllegalArgumentException("The number of x coordinates doesn't match the number of y coordinates");
        }

        if (this.xCoords.length != 2 && this.shapeType == Shapes.CIRCLE) {
            throw new IllegalArgumentException("To define a circle two coordinates are needed (center and one on the circle)");
        } else if (this.xCoords.length != 4 && this.shapeType == Shapes.SQUARE) {
            throw new IllegalArgumentException("To define a rectangle four coordinates are needed (4 vertices)");
        }
    }

}
