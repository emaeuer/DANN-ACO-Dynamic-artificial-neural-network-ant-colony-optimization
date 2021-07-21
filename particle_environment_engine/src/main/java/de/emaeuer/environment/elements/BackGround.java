package de.emaeuer.environment.elements;

import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.elements.shape.ImageShape;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import de.emaeuer.environment.elements.shape.Shapes;

import java.util.Collections;
import java.util.List;

public class BackGround extends AbstractElement {

    private final AbstractEnvironment<?> environment;

    public BackGround(String image, AbstractEnvironment<?> environment) {
        super(new ImageShape(image));
        this.environment = environment;
    }

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        double[] xCoords = new double[] {0, this.environment.getWidth()};
        double[] yCoords = new double[] {0, this.environment.getHeight()};

        ShapeEntity entity = new ShapeEntity(Shapes.IMAGE, getShape(), xCoords, yCoords);
        return Collections.singletonList(entity);
    }
}
