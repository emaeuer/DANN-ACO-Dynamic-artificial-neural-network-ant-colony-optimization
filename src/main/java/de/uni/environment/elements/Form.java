package de.uni.environment.elements;

import java.util.Arrays;
import java.util.stream.IntStream;

public enum Form {

    CIRCLE(),
    PARTICLE(new double[]{0, -1, Math.sqrt(2), -1}, new double[]{0, -1, 0, 1}),
    PIPE(new double[]{0, 0, 1, 1}, new double[]{0, 1, 1, 0}),
    RECTANGLE();

    private final boolean isPrimitive;

    private final double[] xCoords;
    private final double[] yCoords;

    private Form() {
        this.isPrimitive = true;
        this.xCoords = new double[0];
        this.yCoords = new double[0];
    }

    private Form(double[] xCoords, double[] yCoords) {
        this.isPrimitive = false;
        this.xCoords = xCoords;
        this.yCoords = yCoords;
    }

    public boolean isPrimitive() {
        return this.isPrimitive;
    }

    public double[][] getAdjustedXCoords(AbstractElement e) {
        if (e instanceof Particle particle) {
            return getAdjustedXCoordsForParticle(particle);
        } else if (e instanceof Pipe pipe) {
            return getAdjustedXCoordsForPipe(pipe);
        } else {
            return new double[0][0];
        }
    }

    public double[][] getAdjustedYCoords(AbstractElement e) {
        if (e instanceof Particle particle) {
            return getAdjustedYCoordsForParticle(particle);
        } else if (e instanceof Pipe pipe) {
            return getAdjustedYCoordsForPipe(pipe);
        } else {
            return new double[0][0];
        }
    }

    private double[][] getAdjustedXCoordsForParticle(Particle p) {
        double particleX = p.getPosition().getX();
        double radius = p.getRadius();
        double rotationSin = Math.sin(p.getVelocity().angle());
        double rotationCos = Math.cos(p.getVelocity().angle());

        double[][] result = new double[1][];

        result[0] = IntStream.range(0, this.xCoords.length)
                .mapToDouble(i -> particleX + this.xCoords[i] * radius * rotationCos - this.yCoords[i] * radius * rotationSin)
                .toArray();

        return result;
    }

    private double[][] getAdjustedYCoordsForParticle(Particle p) {
        double particleY = p.getPosition().getY();
        double radius = p.getRadius();
        double rotationSin = Math.sin(p.getVelocity().angle());
        double rotationCos = Math.cos(p.getVelocity().angle());

        double[][] result = new double[1][];

        result[0] = IntStream.range(0, this.yCoords.length)
                .mapToDouble(i -> particleY + this.xCoords[i] * radius * rotationSin + this.yCoords[i] * radius * rotationCos)
                .toArray();

        return result;
    }

    private double[][] getAdjustedXCoordsForPipe(Pipe p) {
        double particleX = p.getPosition().getX();
        double width = p.getWidth();

        // pipe consists of top and bottom part
        double[][] result = new double[2][];

        for (int i = 0; i < result.length; i++) {
            result[i] = Arrays.stream(this.xCoords)
                    .map(x -> x * width + particleX)
                    .toArray();
        }

        return result;
    }

    private double[][] getAdjustedYCoordsForPipe(Pipe p) {
        double height = p.getSize().getY();
        double gapPosition = p.getGapPosition();
        double gapSize = p.getGapSize();

        // pipe consists of top and bottom part
        double[][] result = new double[2][];

        result[0] = Arrays.stream(this.yCoords)
                .map(y -> y * gapPosition)
                .toArray();

        result[1] = Arrays.stream(this.yCoords)
                .map(y -> Math.min(y * height + gapPosition + gapSize, height))
                .toArray();

        return result;
    }

}
