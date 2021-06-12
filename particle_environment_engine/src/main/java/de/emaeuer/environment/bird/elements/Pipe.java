package de.emaeuer.environment.bird.elements;

import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.PipeShape;
import de.emaeuer.environment.elements.shape.ShapeEntity;

import java.util.List;

public class Pipe extends AbstractElement {
    private double gapPosition;
    private double gapSize;

    public Pipe() {
        super(new PipeShape());
    }

    public double getGapPosition() {
        return gapPosition;
    }

    public void setGapPosition(double gapPosition) {
        this.gapPosition = gapPosition;
    }

    public double getGapSize() {
        return gapSize;
    }

    public void setGapSize(double gapSize) {
        this.gapSize = gapSize;
    }

    public double getWidth() {
        return getSize().getX();
    }

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((PipeShape) this.getShape()).getShapesForElement(this);
    }

}
