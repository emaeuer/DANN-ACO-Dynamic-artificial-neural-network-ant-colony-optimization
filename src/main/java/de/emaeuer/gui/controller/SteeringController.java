package de.emaeuer.gui.controller;

import de.emaeuer.environment.impl.SteeringEnvironment;
import de.emaeuer.math.Vector2D;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.MouseEvent;

public class SteeringController extends AbstractController<SteeringEnvironment> {

    private final ObjectProperty<Vector2D> target = new SimpleObjectProperty<>();

    @Override
    protected SteeringEnvironment getEnvironmentImplementation() {
        return new SteeringEnvironment(5);
    }

    @Override
    public void initialize() {
        super.initialize();
        this.target.bind(getEnvironment().targetProperty());
    }

    @Override
    public void mouseOver(MouseEvent mouseEvent) {
        if (this.target.isNotNull().get()) {
            this.target.get().setX(mouseEvent.getX());
            this.target.get().setY(mouseEvent.getY());
        }
    }
}
