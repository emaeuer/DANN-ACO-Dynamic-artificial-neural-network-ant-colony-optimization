package de.emaeuer.gui.controller.util;

import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.ImageShape;
import de.emaeuer.environment.elements.shape.Shape;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import org.apache.logging.log4j.core.appender.ScriptAppenderSelector;

import java.util.HashMap;
import java.util.Map;

public class ShapeDrawer {

    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    private ShapeDrawer() {}

    public static void drawElement(AbstractElement element, GraphicsContext context) {
        element.getShapesOfElement()
                .forEach(s -> drawShape(s, context));
    }

    private static void drawShape(ShapeEntity shape, GraphicsContext context) {
        switch (shape.shapeType()) {
            case CIRCLE -> drawCircle(shape, context);
            case SQUARE, POLYGON -> drawPolygon(shape, context);
            case LINE -> drawLine(shape, context);
            case IMAGE -> drawImage(shape, context);
        }
    }

    private static void drawCircle(ShapeEntity shape, GraphicsContext context) {
        double x = shape.xCoords()[0];
        double width = shape.xCoords()[1];
        double y = shape.yCoords()[0];
        double height = shape.yCoords()[1];

        context.setFill(Color.web(shape.color()));
        context.setStroke(Color.web(shape.color()));
        context.fillOval(x, y, width, height);
        context.strokeOval(x, y, width, height);
    }

    private static void drawPolygon(ShapeEntity shape, GraphicsContext context) {
        context.setFill(Color.web(shape.color()));
        context.setStroke(Color.web(shape.color()));
        context.fillPolygon(shape.xCoords(), shape.yCoords(), shape.xCoords().length);
        context.strokePolygon(shape.xCoords(), shape.yCoords(), shape.xCoords().length);
    }

    private static void drawLine(ShapeEntity shape, GraphicsContext context) {
        context.setFill(Color.web(shape.color()));
        context.setStroke(Color.web(shape.color()));
        context.setLineWidth(5);
        context.setLineCap(StrokeLineCap.ROUND);
        context.beginPath();

        for (int i = 0; i < shape.xCoords().length; i++) {
            if (i == 0) {
                context.moveTo(shape.xCoords()[i], shape.yCoords()[i]);
            } else {
                context.lineTo(shape.xCoords()[i], shape.yCoords()[i]);
            }
        }

        context.closePath();
        context.stroke();
    }

    private static void drawImage(ShapeEntity shape, GraphicsContext context) {
        Shape<?> shapeInstance = shape.shape();

        if (shapeInstance instanceof ImageShape imageShape) {
            String imageFile = imageShape.getImageFile();
            Image image = IMAGE_CACHE.computeIfAbsent(imageFile, ShapeDrawer::createImage);

            double x = shape.xCoords()[0];
            double y = shape.yCoords()[0];
            double width = shape.xCoords()[1];
            double height = shape.yCoords()[1];

            if (image != null) {
                context.drawImage(image, x, y, width, height);
            } else {
                context.setFill(Color.web(shape.color()));
                context.setStroke(Color.web(shape.color()));
                context.fillRect(x, y, width, height);
            }
        }
    }

    private static Image createImage(String imageFile) {
        try {
            return new Image(imageFile);
        } catch (Exception e) {
            return null;
        }

    }
}
