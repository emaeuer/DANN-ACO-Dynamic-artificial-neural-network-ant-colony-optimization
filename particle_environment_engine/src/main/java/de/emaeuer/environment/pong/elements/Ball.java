package de.emaeuer.environment.pong.elements;

import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.environment.elements.shape.ShapeEntity;
import de.emaeuer.environment.elements.shape.Shapes;
import de.emaeuer.environment.elements.shape.SimpleShape;

import java.util.List;

public class Ball extends AbstractElement {

    private boolean outOfGame = false;

    public Ball() {
        super(new SimpleShape(Shapes.CIRCLE));
    }

    @Override
    public List<ShapeEntity> getShapesOfElement() {
        return ((SimpleShape) this.getShape()).getShapesForElement(this);
    }

    public boolean isOutOfGame() {
        return outOfGame;
    }

    public void setOutOfGame(boolean outOfGame) {
        this.outOfGame = outOfGame;
    }
}
