package de.emaeuer.gui.controller;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.configuration.ConfigurationUtil;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.evaluation.EvaluationConfiguration;
import de.emaeuer.gui.controller.util.ConfigurationValueInputMapper;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.persistence.BackgroundFileWriter;
import de.emaeuer.persistence.ConfigurationIOHandler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class ConfigurationController {

    private static final FileChooser FILE_CHOOSER = new FileChooser();

    @FXML
    private VBox panel;

    private final ObjectProperty<ConfigurationHandler<EvaluationConfiguration>> configuration = new SimpleObjectProperty<>(new ConfigurationHandler<>(EvaluationConfiguration.class));

    private final ObjectProperty<BackgroundFileWriter> writer = new SimpleObjectProperty<>();

    @FXML
    public void initialize() {
        ConfigurationHandler<OptimizationConfiguration> optimizationConfig = ConfigurationHelper.extractEmbeddedConfiguration(this.configuration.get(), OptimizationConfiguration.class, EvaluationConfiguration.OPTIMIZATION_CONFIGURATION);
        ConfigurationHandler<EnvironmentConfiguration> environmentConfig = ConfigurationHelper.extractEmbeddedConfiguration(this.configuration.get(), EnvironmentConfiguration.class, EvaluationConfiguration.ENVIRONMENT_CONFIGURATION);
        optimizationConfig.setName("OPTIMIZATION_CONFIGURATION");
        environmentConfig.setName("ENVIRONMENT_CONFIGURATION");
        this.configuration.get().setName("CONFIGURATION");

        panel.getChildren().addAll(ConfigurationValueInputMapper.createPaneForConfiguration(this.configuration.get(), this::refreshPanel, "Configuration", panel));
    }

    public void writeConfig() {
        if (this.writer.isNull().get()) {
            return;
        }

        ConfigurationUtil.printConfiguration(this.configuration.get(), this.writer.get());
    }

    public void refreshPanel() {
        this.panel.getChildren().clear();
        initialize();
    }

    public void setDisable(boolean value) {
        this.panel.setDisable(value);
    }

    public ObjectProperty<BackgroundFileWriter> writerProperty() {
        return writer;
    }

    public void saveConfig() {
        if (this.configuration.isNotNull().get()) {
            File file = FILE_CHOOSER.showSaveDialog(panel.getScene().getWindow());
            if (file != null) {
                ConfigurationIOHandler.exportConfiguration(file, this.configuration.get());
            }
        }
    }

    public void loadConfig() {
        if (this.configuration.isNotNull().get()) {
            File file = FILE_CHOOSER.showOpenDialog(panel.getScene().getWindow());
            if (file != null && file.exists()) {
                ConfigurationIOHandler.importConfiguration(file, configuration.get());
                refreshPanel();
            }
        }
    }

    public ObjectProperty<ConfigurationHandler<EvaluationConfiguration>> configurationProperty() {
        return configuration;
    }
}
