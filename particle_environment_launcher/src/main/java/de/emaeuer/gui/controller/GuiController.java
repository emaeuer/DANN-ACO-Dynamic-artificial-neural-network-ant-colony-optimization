package de.emaeuer.gui.controller;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
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

        this.environmentAreaController.environmentConfigurationProperty().bind(this.configurationPanelController.environmentConfigurationProperty());
        this.environmentAreaController.optimizationConfigurationProperty().bind(this.configurationPanelController.optimizationConfigurationProperty());
        this.environmentAreaController.optimizationStateProperty().bind(this.statePanelController.stateProperty());
        this.environmentAreaController.updatedProperty().addListener((v, o, n) -> handleProgression());
        this.environmentAreaController.finishedProperty().addListener((v, o, n) -> handleEnvironmentEnd(n));
        this.playButton.disableProperty().bind(this.environmentAreaController.finishedProperty());

        this.configurationPanelController.writerProperty().bind(this.statePanelController.writerProperty());

        this.environmentAreaController.initializeController();
    }

    private void handleProgression() {
        this.statePanelController.refreshPanel();
    }

    private void handleEnvironmentEnd(boolean isFinished) {
        if (isFinished) {
            // handle end
            pause();
        }
    }

    private boolean isFirstStart = true;

    @FXML
    public void start() {
        if (this.isFirstStart) {
            isFirstStart = false;
            this.statePanelController.init();
            this.configurationPanelController.writeConfig();
            this.configurationPanelController.setDisable(true);
        }

        this.environmentAreaController.startEnvironment();
        togglePlaying(true);
    }

    @FXML
    public void pause() {
        this.environmentAreaController.pauseEnvironment();

        togglePlaying(false);
    }

    @FXML
    public void reset() {
        this.configurationPanelController.setDisable(false);
        this.statePanelController.reset();
        this.environmentAreaController.restartEnvironment();
        this.isFirstStart = true;

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
    public void changeMultiEntityMode(ActionEvent e) {
        this.environmentAreaController.toggleEntityMode();
        ((Node) e.getSource()).pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), this.environmentAreaController.isSingleEntityMode());
    }

    public void switchDisplayMode(ActionEvent e) {
        this.environmentAreaController.toggleVisualMode();
        ((Node) e.getSource()).pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), !this.environmentAreaController.isVisualMode());
    }

    @FXML
    public void save() {
        this.configurationPanelController.saveConfig();
    }

    @FXML
    public void load() {
        this.configurationPanelController.loadConfig();
    }
}
