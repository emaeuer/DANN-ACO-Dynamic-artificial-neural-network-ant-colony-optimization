package de.emaeuer.gui.controller;

import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.gui.GuiActions;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public abstract class AbstractController<T extends AbstractEnvironment> implements GuiActions {

    @FXML
    public BorderPane root;

    @FXML
    private Canvas canvas;

    @FXML
    private LineChart<Integer, Double> scoreBoard;

    private final T environment;

    public AbstractController() {
        this.environment = getEnvironmentImplementation();
    }

    protected abstract T getEnvironmentImplementation();

    @FXML
    public void initialize() {
        this.canvas.widthProperty().setValue(800);
        this.canvas.heightProperty().setValue(800);

        this.environment.widthProperty().bind(this.canvas.widthProperty());
        this.environment.heightProperty().bind(this.canvas.heightProperty());

        this.scoreBoard.dataProperty().bind(this.environment.fitnessDataProperty());

        AnimationTimer frameTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                nextFrame();
            }
        };
        frameTimer.start();
    }

    protected void nextFrame() {
        environment.update();
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

    protected T getEnvironment() {
        return environment;
    }

    protected GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

}
