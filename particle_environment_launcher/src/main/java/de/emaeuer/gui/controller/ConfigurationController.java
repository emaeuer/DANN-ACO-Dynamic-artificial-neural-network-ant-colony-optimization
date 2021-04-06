package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.gui.controller.util.ConfigurationValueInputMapper;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class ConfigurationController {

    @FXML
    private VBox panel;

    private final ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> environmentConfiguration = new SimpleObjectProperty<>(new ConfigurationHandler<>(EnvironmentConfiguration.class));
    private final ObjectProperty<ConfigurationHandler<OptimizationConfiguration>> optimizationConfiguration = new SimpleObjectProperty<>(new ConfigurationHandler<>(OptimizationConfiguration.class));

    @FXML
    public void initialize() {
        panel.getChildren().addAll(ConfigurationValueInputMapper.createPaneForConfiguration(getOptimizationConfiguration(), this::refreshPanel, "Optimization configuration"));
        panel.getChildren().addAll(ConfigurationValueInputMapper.createPaneForConfiguration(getEnvironmentConfiguration(), this::refreshPanel, "Environment configuration"));
    }

    public void refreshPanel() {
        this.panel.getChildren().clear();
        initialize();
    }

    public void setDisable(boolean value) {
        this.panel.setDisable(value);
    }

    public ConfigurationHandler<EnvironmentConfiguration> getEnvironmentConfiguration() {
        return environmentConfiguration.get();
    }

    public ConfigurationHandler<OptimizationConfiguration> getOptimizationConfiguration() {
        return optimizationConfiguration.get();
    }

    public ObjectProperty<ConfigurationHandler<EnvironmentConfiguration>> environmentConfigurationProperty() {
        return environmentConfiguration;
    }

    public ObservableValue<? extends ConfigurationHandler<OptimizationConfiguration>> optimizationConfigurationProperty() {
        return optimizationConfiguration;
    }
}
