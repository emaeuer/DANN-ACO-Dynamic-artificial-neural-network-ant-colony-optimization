package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.gui.controller.util.ConfigurationValueInputMapper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class ConfigurationController {

    @FXML
    private VBox panel;

    private final ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> configuration = new SimpleObjectProperty<>(new ConfigurationHandler<>(EnvironmentConfiguration.class));

    @FXML
    public void initialize() {
        panel.getChildren().addAll(ConfigurationValueInputMapper.createPaneForConfiguration(getConfiguration(), this::refreshPanel, "Environment configuration"));
    }

    public void refreshPanel() {
        this.panel.getChildren().clear();
        initialize();
    }

    public void setDisable(boolean value) {
        this.panel.setDisable(value);
    }

    public ConfigurationHandler<EnvironmentConfiguration> getConfiguration() {
        return configuration.get();
    }

    public ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> configurationProperty() {
        return configuration;
    }

}
