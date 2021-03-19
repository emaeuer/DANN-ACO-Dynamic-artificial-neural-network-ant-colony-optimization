package de.emaeuer.gui.controller.util;

import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ShapeDrawer {

    private ShapeDrawer() {}

    public static void drawElement(AbstractElement element, GraphicsContext context) {
        prepareColors(element, context);
        element.getShapesOfElement()
                .forEach(s -> drawShape(s, context));
    }

    private static void prepareColors(AbstractElement element, GraphicsContext context) {
        context.setFill(Color.web(element.getColor()));
        context.setStroke(Color.web(element.getBorderColor()));
    }

    private static void drawShape(ShapeEntity shape, GraphicsContext context) {
        switch (shape.shape()) {
            case CIRCLE -> drawCircle(shape, context);
            case SQUARE, POLYGON -> drawPolygon(shape, context);
        }
    }

    private static void drawCircle(ShapeEntity shape, GraphicsContext context) {
        double x = shape.xCoords()[0];
        double width = shape.xCoords()[1];
        double y = shape.yCoords()[0];
        double height = shape.yCoords()[1];

        context.fillOval(x, y, width, height);
        context.strokeOval(x, y, width, height);
    }

    private static void drawPolygon(ShapeEntity shape, GraphicsContext context) {
        context.fillPolygon(shape.xCoords(), shape.yCoords(), shape.xCoords().length);
        context.strokePolygon(shape.xCoords(), shape.yCoords(), shape.xCoords().length);
    }
}
