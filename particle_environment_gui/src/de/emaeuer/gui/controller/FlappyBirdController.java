package de.emaeuer.gui.controller;

import de.emaeuer.environment.impl.FlappyBirdEnvironment;
import javafx.event.ActionEvent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class FlappyBirdController extends AbstractController<FlappyBirdEnvironment> {

    @Override
    protected FlappyBirdEnvironment getEnvironmentImplementation() {
        return new FlappyBirdEnvironment(300, 100,400);
    }

    @Override
    protected void initializeController() {
        super.initializeController();

        getEnvironment().allBirdsDeadProperty().addListener((v, o, newValue) -> {
            if (newValue) {
                getEnvironment().restart();
            }
        });
    }

    @Override
    protected void nextFrame() {
        super.nextFrame();

        getEnvironment().getPipes().forEach(this::drawElement);
    }

}
