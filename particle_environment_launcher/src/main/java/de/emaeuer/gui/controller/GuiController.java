package de.emaeuer.gui.controller;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class GuiController {

    @FXML
    private VBox statePanel;

    @FXML
    private VBox logPanel;

    @FXML
    private VBox configurationPanel;

    @FXML
    private Button playButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button entityModeButton;

    @FXML
    private EnvironmentController environmentAreaController;

    @FXML
    private ConfigurationController configurationPanelController;

    @FXML
    private StateController statePanelController;

    @FXML
    private Button plotButton;

    @FXML
    private Button logButton;

    @FXML
    private Button settingButton;

    @FXML
    private Label speedDisplay;

    @FXML
    public void initialize() {
        this.pauseButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("paused"), true);
        this.speedDisplay.textProperty().bind(this.environmentAreaController.speedProperty());

        showSettingPanel();

        this.environmentAreaController.configurationProperty().bind(this.configurationPanelController.configurationProperty());
        this.environmentAreaController.stateProperty().bind(this.statePanelController.stateProperty());

        this.environmentAreaController.restartedProperty().addListener((v, o, n) -> handleEnvironmentRestart(n));
    }

    private void handleEnvironmentRestart(boolean restarted) {
        if (restarted) {
            this.statePanelController.refreshPanel();
        }
    }

    @FXML
    public void startEnvironment() {
        this.configurationPanelController.setDisable(true);
        this.environmentAreaController.startEnvironment();

        togglePlaying(true);
    }

    @FXML
    public void pauseEnvironment() {
        this.environmentAreaController.pauseEnvironment();

        togglePlaying(false);
    }

    @FXML
    public void restartEnvironment() {
        this.configurationPanelController.setDisable(false);
        this.statePanelController.reset();
        this.environmentAreaController.restartEnvironment();

        togglePlaying(false);
    }

    @FXML
    public void increaseEnvironmentSpeed() {
        this.environmentAreaController.increaseEnvironmentSpeed();
    }

    @FXML
    public void decreaseEnvironmentSpeed() {
        this.environmentAreaController.decreaseEnvironmentSpeed();
    }

    @FXML
    public void showPlotPanel() {
        this.statePanel.toFront();
        markButtonForSelectedButton(this.plotButton);
    }

    @FXML
    public void showLogPanel() {
        this.logPanel.toFront();
        markButtonForSelectedButton(this.logButton);
    }

    @FXML
    public void showSettingPanel() {
        this.configurationPanel.toFront();
        markButtonForSelectedButton(this.settingButton);
    }

    private void togglePlaying(boolean isPlaying) {
        this.playButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("playing"), isPlaying);
        this.pauseButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("paused"), !isPlaying);
    }

    private void markButtonForSelectedButton(Button selected) {
        this.plotButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("shown"), this.plotButton == selected);
        this.logButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("shown"), this.logButton == selected);
        this.settingButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("shown"), this.settingButton == selected);
    }

    @FXML
    public void changeMultiEntityMode() {
        this.environmentAreaController.toggleEntityMode();
        this.entityModeButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), this.environmentAreaController.isSingleEntityMode());
    }
}
