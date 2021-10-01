package de.emaeuer.environment.bird.elements.shape;

import de.emaeuer.environment.bird.elements.FlappyBird;
import de.emaeuer.environment.elements.shape.ImageShape;
import de.emaeuer.environment.elements.shape.Shape;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import de.emaeuer.environment.elements.shape.Shapes;

import java.util.Collections;
import java.util.List;

public class FlappyBirdShape implements Shape<FlappyBird> {

    @Override
    public List<ShapeEntity> getShapesForElement(FlappyBird element) {
        double width = element.getSize().getX();
        double height = element.getSize().getY();
        double x = element.getPosition().getX() - width / 2;
        double y = element.getPosition().getY() - height / 2;

        double[] xCoords = new double[] {x, width};
        double[] yCoords = new double[] {y, height};

        ImageShape imageShape;
        if (element.getVelocity().getY() > 0) {
            imageShape = new ImageShape(getClass().getResource("/flappy_bird/bird_down.png").toExternalForm());
        } else {
            imageShape = new ImageShape(getClass().getResource("/flappy_bird/bird_up.png").toExternalForm());
        }

        ShapeEntity entity = new ShapeEntity(Shapes.IMAGE, imageShape, xCoords, yCoords);

        return Collections.singletonList(entity);
    }

}
