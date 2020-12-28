package de.uni.environment.elements;

public class Pipe extends AbstractElement {
    private double gapPosition;
    private double gapSize;

    public Pipe() {
        super(Form.PIPE);
    }

    public double getGapPosition() {
        return gapPosition;
    }

    public void setGapPosition(double gapPosition) {
        this.gapPosition = gapPosition;
    }

    public double getGapSize() {
        return gapSize;
    }

    public void setGapSize(double gapSize) {
        this.gapSize = gapSize;
    }

    public double getWidth() {
        return getSize().getX();
    }

}
