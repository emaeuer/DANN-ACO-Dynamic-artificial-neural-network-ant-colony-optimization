package de.emaeuer.environment.elements.builder;

import de.emaeuer.environment.elements.Pipe;

public class PipeBuilder extends ElementBuilder<Pipe, PipeBuilder> {

    public PipeBuilder() {
        super();
    }

    @Override
    protected Pipe getElementImplementation() {
        return new Pipe();
    }

    @Override
    protected PipeBuilder getThis() {
        return this;
    }

    public PipeBuilder gapPosition(double gapPosition) {
        getElement().setGapPosition(gapPosition);
        return this;
    }

    public PipeBuilder gapSize(double gapSize) {
        getElement().setGapSize(gapSize);
        return this;
    }

}
