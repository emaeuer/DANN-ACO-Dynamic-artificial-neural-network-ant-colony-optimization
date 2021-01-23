package de.emaeuer.gui.controller;

import de.emaeuer.environment.impl.FlappyBirdEnvironment;

public class FlappyBirdController extends AbstractController<FlappyBirdEnvironment> {

    public FlappyBirdController() {
        getEnvironment().allBirdsDeadProperty().addListener((v, o, newValue) -> {
            if (newValue) {
                getEnvironment().restart();
            }
        });
    }

    @Override
    protected FlappyBirdEnvironment getEnvironmentImplementation() {
        return new FlappyBirdEnvironment(300, 100,400);
    }

    @Override
    protected void nextFrame() {
        super.nextFrame();

        getEnvironment().getPipes().forEach(this::drawElement);
    }
}
