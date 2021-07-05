package de.emaeuer.gui.controller.util;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariable;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.*;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ConfigurationValueInputMapper {

    private ConfigurationValueInputMapper() {}

    public static List<Node> createPaneForConfiguration(ConfigurationHandler<?> configuration, Runnable action, String name, Node parent) {
        if (configuration == null) {
            return Collections.emptyList();
        }

        List<Node> nodes = new ArrayList<>();

        VBox boxWithDirectInputs = new VBox();
        boxWithDirectInputs.setSpacing(10);
        boxWithDirectInputs.getStyleClass().add("output_card");
        boxWithDirectInputs.getChildren().add(createCardTitle(name));
        nodes.add(boxWithDirectInputs);

        // create inputs for everything that is not an embedded configuration
        configuration.getConfigurationValues()
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().isDisabled())
                .filter(e -> !EmbeddedConfiguration.class.equals(e.getKey().getValueType()))
                .map(e -> ConfigurationValueInputMapper.mapConfigurationToInput(e, action, configuration))
                .forEach(n -> boxWithDirectInputs.getChildren().add(n));

        // create components for embedded configurations
        configuration.getConfigurationValues()
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().isDisabled())
                .filter(e -> EmbeddedConfiguration.class.equals(e.getKey().getValueType()))
                .filter(e -> e.getValue() != null)
                .map(e -> createPaneForConfiguration(((EmbeddedConfiguration<?>) e.getValue()).getValue(),
                        action, e.getKey().getName(), parent))
                .forEach(nodes::addAll);

        return nodes;
    }

    public static Node mapConfigurationToInput(Entry<? extends DefaultConfiguration<?>, AbstractConfigurationValue<?>> configuration, Runnable action, ConfigurationHandler<?> configurationHandler) {
        DefaultConfiguration<?> configType = configuration.getKey();
        AbstractConfigurationValue<?> configValue = configuration.getValue();

        if (configValue instanceof IntegerConfigurationValue intConfigValue) {
            return createIntegerInput(configType, intConfigValue, action, configurationHandler);
        } else if (configValue instanceof BooleanConfigurationValue boolConfigValue) {
            return createBooleanInput(configType, boolConfigValue, action, configurationHandler);
        } else if (configValue instanceof StringConfigurationValue stringConfigValue) {
            return createStringInput(configType, stringConfigValue, action, configurationHandler);
        } else if (configValue instanceof ExpressionConfigurationValue exprConfigValue) {
            return createExpressionInput(configType, exprConfigValue, action, configurationHandler);
        } else if (configValue instanceof DoubleConfigurationValue doubleConfigValue) {
            return  createDoubleInput(configType, doubleConfigValue, action, configurationHandler);
        }  else if (configValue instanceof  NumericListConfigurationValue listConfigValue) {
            return createListInput(configType, listConfigValue, action, configurationHandler);
        }

        return new Label(String.format("### Unknown value Type %s for %s ###", configValue.getClass().getSimpleName(), configType.getName()));
    }

    private static Node createIntegerInput(DefaultConfiguration<?> config, IntegerConfigurationValue value, Runnable action, ConfigurationHandler<?> configurationHandler) {
        Spinner<Integer> spinner = new Spinner<>(value.getMin(), value.getMax(), Integer.parseInt(value.getStringRepresentation()));
        spinner.valueProperty().addListener((v, o, n) -> configurationHandler.setValue(config.getKeyName(), n));
        spinner.setEditable(true);
        spinner.setDisable(value.isDisabled());

        if (config.refreshNecessary()) {
            spinner.valueProperty().addListener((v, o, n) -> action.run());
        }

        return createStandardInput(config.getName(), spinner);
    }

    private static Node createBooleanInput(DefaultConfiguration<?> config, BooleanConfigurationValue value, Runnable action, ConfigurationHandler<?> configurationHandler) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(Boolean.parseBoolean(value.getStringRepresentation()));
        checkBox.selectedProperty().addListener((v, o, n) -> configurationHandler.setValue(config.getKeyName(), n));
        checkBox.setDisable(value.isDisabled());

        if (config.refreshNecessary()) {
            checkBox.selectedProperty().addListener((v, o, n) -> action.run());
        }

        return createStandardInput(config.getName(), checkBox);
    }

    private static Node createStringInput(DefaultConfiguration<?> config, StringConfigurationValue value, Runnable action, ConfigurationHandler<?> configurationHandler) {
        if (value.getPossibleValues().isEmpty()) {
            TextField field = new TextField(value.getStringRepresentation());
            field.textProperty().addListener((v, o, n) -> configurationHandler.setValue(config.getKeyName(), n));
            field.setDisable(value.isDisabled());

            if (config.refreshNecessary()) {
                field.textProperty().addListener((v, o, n) -> action.run());
            }

            return createStandardInput(config.getName(), field);
        } else {
            ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(value.getPossibleValues()));
            comboBox.setValue(value.getStringRepresentation());
            comboBox.valueProperty().addListener((v, o, n) -> configurationHandler.setValue(config.getKeyName(), n));
            comboBox.setDisable(value.isDisabled());

            if (config.refreshNecessary()) {
                comboBox.valueProperty().addListener((v, o, n) -> action.run());
            }

            return createStandardInput(config.getName(), comboBox);
        }
    }

    private static Node createDoubleInput(DefaultConfiguration<?> config, DoubleConfigurationValue value, Runnable action, ConfigurationHandler<?> configurationHandler) {
        TextField field = new TextField(value.getStringRepresentation());
        field.setDisable(value.isDisabled());
        field.textProperty().addListener((v, o, n) -> {
            if (inputIsValid(field, n, "[+-]?([0-9]+[.])?[0-9]+")) {
                configurationHandler.setValue(config.getKeyName(), n);
            }
        });

        if (config.refreshNecessary()) {
            field.textProperty().addListener((v, o, n) -> action.run());
        }

        return createStandardInput(config.getName(), field);
    }

    private static Node createListInput(DefaultConfiguration<?> config, NumericListConfigurationValue value, Runnable action, ConfigurationHandler<?> configurationHandler) {
        TextField field = new TextField(value.getStringRepresentation());
        field.setDisable(value.isDisabled());
        field.textProperty().addListener((v, o, n) -> {
            if (inputIsValid(field, n, "(([+-]?([0-9]+[.])?[0-9]+)(,|$){1}\\s*)*")) {
                configurationHandler.setValue(config.getKeyName(), n);
            }
        });

        if (config.refreshNecessary()) {
            field.textProperty().addListener((v, o, n) -> action.run());
        }

        return createStandardInput(config.getName(), field);
    }

    private static boolean inputIsValid(TextField field, String value, String regex) {
        if (!value.matches(regex)) {
            field.pseudoClassStateChanged(PseudoClass.getPseudoClass("invalid"), true);
            return false;
        }
        field.pseudoClassStateChanged(PseudoClass.getPseudoClass("invalid"), false);
        return true;
    }

    private static Node createExpressionInput(DefaultConfiguration<?> config, ExpressionConfigurationValue value, Runnable action, ConfigurationHandler<?> configurationHandler) {
        TextField field = new TextField(value.getStringRepresentation());
        field.setTooltip(creatToolTipForExpression(value.getVariables()));
        field.setDisable(value.isDisabled());
        field.textProperty().addListener((v, o, n) -> configurationHandler.setValue(config.getKeyName(), n));

        if (config.refreshNecessary()) {
            field.textProperty().addListener((v, o, n) -> action.run());
        }

        return createStandardInput(config.getName(), field);
    }

    private static Tooltip creatToolTipForExpression(Class<? extends ConfigurationVariable> variables) {
        String tip = Arrays.stream(variables.getEnumConstants())
                .map(v -> String.format("%-3s- %s\n", v.getEquationAbbreviation(), v.getName()))
                .collect(Collectors.joining());

        return new Tooltip(tip);
    }

    private static VBox createStandardInput(String name, Node inputElement) {
        VBox box = new VBox();
        box.getStyleClass().add("input_box");
        box.getChildren().add(new Label(name));
        box.getChildren().add(inputElement);
        return box;
    }

    private static Label createCardTitle(String titleText) {
        Label title =  new Label(titleText);
        title.getStyleClass().add("card_title");

        return title;
    }
}
