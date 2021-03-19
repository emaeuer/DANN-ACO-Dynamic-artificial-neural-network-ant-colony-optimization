package de.emaeuer.gui.controller;

import de.emaeuer.logging.LoggingProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class LogController {

    @FXML
    private TextArea logText;

    @FXML
    public void initialize() {
        LoggingProperty.bindToLog(this.logText.textProperty());
        LoggingProperty.addListener(e -> {
            logText.selectPositionCaret(logText.getLength());
            logText.deselect();
        });
    }

}
