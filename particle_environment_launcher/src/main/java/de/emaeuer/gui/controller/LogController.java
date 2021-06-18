package de.emaeuer.gui.controller;

import de.emaeuer.logging.LoggingProperty;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

import java.util.Collection;

public class LogController {

    @FXML
    public Spinner<Integer> logSize;

    @FXML
    public Spinner<Integer> refreshInterval;

    @FXML
    private TextArea logText;

    private Timeline logRefresher;

    private final StringBuffer buffer = new StringBuffer();

    @FXML
    public void initialize() {
        createPeriodicRefresher();


        this.refreshInterval.valueProperty().addListener((p, o, n) -> createPeriodicRefresher());

    }

    private void createPeriodicRefresher() {
        if (logRefresher != null) {
            logRefresher.pause();
        }
        this.logRefresher = new Timeline(new KeyFrame(Duration.seconds(getSecondsBetweenLogUpdates()), this::refreshLog));
        this.logRefresher.setCycleCount(Animation.INDEFINITE);
        this.logRefresher.play();
    }

    private void refreshLog(ActionEvent actionEvent) {
        Collection<String> logEntries = LoggingProperty.retrieveNewLogEntries();

        if (logEntries.isEmpty()) {
            return;
        }

        for (String logEntry : logEntries) {
            this.buffer.append(logEntry);
        }

        if (this.buffer.length() > getMaxNumberOfCharacters()) {
            this.buffer.delete(0, this.buffer.length() - getMaxNumberOfCharacters());
        }

        this.logText.setText(this.buffer.toString());
        this.logText.setScrollTop(Double.MAX_VALUE);
    }

    private double getSecondsBetweenLogUpdates() {
        return this.refreshInterval.getValue().doubleValue();
    }

    private int getMaxNumberOfCharacters() {
        return this.logSize.getValue();
    }

}
