package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.balance.twodim.element.TwoDimensionalCart;

import java.util.List;

public class TwoDimensionalCartShape implements Shape<TwoDimensionalCart> {

    private static final double SCALE_TOLERANCE = 50;

    @Override
    public List<ShapeEntity> getShapesForElement(TwoDimensionalCart element) {
        double scale = calculateScale(element);

        ShapeEntity cart = getShapeForCart(element, scale);
        ShapeEntity pole = getShapeForPole(element, scale);

        return List.of(cart, pole);
    }

    private double calculateScale(TwoDimensionalCart element) {
        double trackSize = element.getData().trackLength();
        double xScale = (element.getEnvironment().getWidth() - SCALE_TOLERANCE) / trackSize;
        double yScale = (element.getEnvironment().getHeight() - SCALE_TOLERANCE) / trackSize;

        return Math.max(xScale, yScale);
    }

    private ShapeEntity getShapeForCart(TwoDimensionalCart element, double scale) {
        double width = element.getSize().getX();
        double height = element.getSize().getY();
        double x = getAdjustedX(element, scale);
        double y = getAdjustedY(element, scale);

        double[] xCoords = new double[] {x, width};
        double[] yCoords = new double[] {y, height};

        return new ShapeEntity(BasicShape.CIRCLE, xCoords, yCoords);
    }

    private ShapeEntity getShapeForPole(TwoDimensionalCart element, double scale) {
        double startX = getAdjustedX(element, scale) + element.getSize().getX() / 2;
        double startY = getAdjustedY(element, scale) + element.getSize().getY() / 2;

        double endX = startX + Math.sin(Math.toRadians(element.getPoleXAngle())) * 1000;
        double endY = startY + Math.sin(Math.toRadians(element.getPoleYAngle())) * 1000;

        double[] xCoords = new double[] {startX, endX};
        double[] yCoords = new double[] {startY, endY};

        return new ShapeEntity(BasicShape.LINE, xCoords, yCoords);
    }

    private double getAdjustedX(TwoDimensionalCart element, double scale) {
        double x = element.getPosition().getX();
        x = (x - element.getEnvironment().getWidth() / 2) * scale;
        x -= element.getSize().getX() / 2;
        x += element.getEnvironment().getWidth() / 2;
        return x;
    }

    private double getAdjustedY(TwoDimensionalCart element, double scale) {
        double y = element.getPosition().getY();
        y = (y - element.getEnvironment().getHeight() / 2) * scale;
        y -= element.getSize().getY() / 2;
        y += element.getEnvironment().getHeight() / 2;
        return y;
    }

}
