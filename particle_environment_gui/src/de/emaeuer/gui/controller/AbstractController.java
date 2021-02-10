package de.emaeuer.gui.controller;

import de.emaeuer.logging.LoggingProperty;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.gui.GuiActions;
import javafx.animation.AnimationTimer;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public abstract class AbstractController<T extends AbstractEnvironment> implements GuiActions {

    @FXML
    public BorderPane root;

    @FXML
    private Canvas canvas;

    @FXML
    private StackPane sidePanel;

    @FXML
    private Button playButton;

    @FXML
    private Button pauseButton;

    @FXML
    private TextArea logText;

    @FXML
    private LineChart<Integer, Double> scoreBoard;

    @FXML
    private VBox plotPanel;

    @FXML
    private VBox logPanel;

    @FXML
    private VBox settingPanel;

    @FXML
    private VBox acoPanel;

    private T environment;

    private AnimationTimer frameTimer;

    private double speed = 1;
    private boolean evenFrameNumber = false;

    protected abstract T getEnvironmentImplementation();

    @FXML
    public void initialize() {
        this.canvas.widthProperty().setValue(800);
        this.canvas.heightProperty().setValue(800);

        this.frameTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                nextFrame();
            }
        };

        this.pauseButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("paused"), true);

        LoggingProperty.bindToLog(this.logText.textProperty());
        LoggingProperty.addListener(e -> {
            logText.selectPositionCaret(logText.getLength());
            logText.deselect();
        });

        initializeController();
    }

    protected void initializeController() {
        if (this.environment != null) {
            this.environment.fitnessDataProperty().unbind();
            this.environment.widthProperty().unbind();
            this.environment.heightProperty().unbind();
        }

        this.environment = getEnvironmentImplementation();

        this.environment.widthProperty().bind(this.canvas.widthProperty());
        this.environment.heightProperty().bind(this.canvas.heightProperty());

        this.scoreBoard.dataProperty().bind(this.environment.fitnessDataProperty());
    }

    protected void nextFrame() {
        this.evenFrameNumber = !this.evenFrameNumber;

        double floored = Math.floor(this.speed);
        for (int i = 0; i < floored; i++) {
            environment.update();
        }

        if (this.evenFrameNumber && this.speed - floored > 0) {
            environment.update();
        }

        GraphicsContext context = getGraphicsContext();
        context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        environment.getParticles().forEach(this::drawElement);
    }

    protected void drawElement(AbstractElement element) {
        GraphicsContext context = getGraphicsContext();

        context.setFill(Color.web(element.getColor()));
        context.setStroke(Color.web(element.getBorderColor()));

        double[][] x = element.getForm().getAdjustedXCoords(element);
        double[][] y = element.getForm().getAdjustedYCoords(element);

        for (int i = 0; i < x.length; i++) {
            context.fillPolygon(x[i], y[i], x[i].length);
            context.strokePolygon(x[i], y[i], x[i].length);
        }
    }

    @Override
    public void mouseOver(MouseEvent event) {

    }

    @Override
    public void mouseClicked(MouseEvent event) {

    }

    @Override
    public void keyReleased(KeyEvent event) {

    }

    @Override
    public void keyPressed(KeyEvent event) {

    }

    @FXML
    public void playEnvironment() {
        this.frameTimer.start();
        this.playButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("playing"), true);
        this.pauseButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("paused"), false);
    }

    @FXML
    public void pauseEnvironment() {
        this.frameTimer.stop();
        this.playButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("playing"), false);
        this.pauseButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("paused"), true);
    }

    @FXML
    public void restartEnvironment() {
        frameTimer.stop();
        initializeController();
        frameTimer.start();
    }

    @FXML
    public void increaseEnvironmentSpeed() {
        speed += speed >= 10 ? 0 : 0.5; // maximum speed is 10
    }

    @FXML
    public void decreaseEnvironmentSpeed() {
        speed -= speed <= 0.5 ? 0 : 0.5; // minimum speed is 0.5
    }

    @FXML
    public void showPlotPanel() {
        this.plotPanel.toFront();
    }

    @FXML
    public void showLogPanel() {
        this.logPanel.toFront();
    }

    @FXML
    public void showAcoPanel(ActionEvent event) {
        this.acoPanel.toFront();
    }

    @FXML
    public void showSettingPanel(ActionEvent event) {
        this.settingPanel.toFront();
    }

    protected T getEnvironment() {
        return environment;
    }

    protected GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

}
