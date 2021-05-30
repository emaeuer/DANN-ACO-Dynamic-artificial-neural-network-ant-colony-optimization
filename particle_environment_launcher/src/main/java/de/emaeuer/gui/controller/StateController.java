package de.emaeuer.gui.controller;

import de.emaeuer.gui.controller.util.StateValueOutputMapper;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.persistence.BackgroundFileWriter;
import de.emaeuer.state.StateHandler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StateController {

    private final static Logger LOG = LogManager.getLogger(StateController.class);

    @FXML
    private VBox panel;

    private final ObjectProperty<StateHandler<OptimizationState>> state = new SimpleObjectProperty<>(new StateHandler<>(OptimizationState.class));

    private final ObjectProperty<BackgroundFileWriter> writer = new SimpleObjectProperty<>();

    private StateValueOutputMapper mapper = new StateValueOutputMapper(getState());

    public void init() {
        panel.getChildren().clear();

        try {
            String fileName = "temp/execution_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(new Date()) + ".txt";
            this.writer.set(new BackgroundFileWriter(fileName));
            this.state.set(new StateHandler<>(OptimizationState.class, writer.get()));
        } catch (IOException e) {
            LOG.warn("Disabled data export because the creation of the writer failed", e);
            this.state.set(new StateHandler<>(OptimizationState.class));
        }

        this.state.get().setName("GENERAL_STATE");
        this.mapper = new StateValueOutputMapper(getState());
        refreshPanel();
    }

    public synchronized void refreshPanel() {
        // refresh and if new nodes were created add them
        this.panel.getChildren().addAll(mapper.refreshProperties());
    }

    public void reset() {
        try {
            this.state.get().close();
        } catch (Exception e) {
            LOG.warn("Failed to reset state", e);
        }
        init();
    }

    public StateHandler<OptimizationState> getState() {
        return state.get();
    }

    public ObjectProperty<StateHandler<OptimizationState>> stateProperty() {
        return state;
    }

    public ObjectProperty<BackgroundFileWriter> writerProperty() {
        return writer;
    }
}
