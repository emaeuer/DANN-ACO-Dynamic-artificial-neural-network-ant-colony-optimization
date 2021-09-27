package de.emaeuer.environment.pong.elements;

import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import de.emaeuer.environment.elements.shape.Shapes;
import de.emaeuer.environment.elements.shape.SimpleShape;

import java.util.List;

public class Paddle extends AbstractElement {

    private long stepNumber = 0;

    public Paddle() {
        super(new SimpleShape(Shapes.SQUARE));
    }

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((SimpleShape) this.getShape()).getShapesForElement(this);
    }

    @Override
    public void step() {
        super.step();
        this.stepNumber++;
    }

    public long getStepNumber() {
        return this.stepNumber;
    }
}
