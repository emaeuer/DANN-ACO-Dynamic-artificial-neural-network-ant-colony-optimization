package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.gui.controller.util.StateValueOutputMapper;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

public class StateController {

    @FXML
    private VBox panel;

    private final ObjectProperty<StateHandler<OptimizationState>> state = new SimpleObjectProperty<>(new StateHandler<>(OptimizationState.class));

    private StateValueOutputMapper mapper = new StateValueOutputMapper(getState());

    @FXML
    public void initialize() {
        panel.getChildren().clear();
        this.state.set(new StateHandler<>(OptimizationState.class));
        this.mapper = new StateValueOutputMapper(getState());
        refreshPanel();
    }

    public synchronized void refreshPanel() {
        // refresh and if new nodes were created add them
        this.panel.getChildren().addAll(mapper.refreshProperties());
    }

    public void reset() {
        initialize();
    }

    public StateHandler<OptimizationState> getState() {
        return state.get();
    }

    public ObjectProperty<StateHandler<OptimizationState>> stateProperty() {
        return state;
    }

}
