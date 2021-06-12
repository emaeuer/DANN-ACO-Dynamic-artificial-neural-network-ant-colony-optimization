package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.elements.Particle;

import java.util.List;
import java.util.stream.IntStream;

public class ParticleShape implements Shape<Particle> {

    private static final double[] X_COORDS = new double[]{0, -1, Math.sqrt(2), -1};
    private static final double[] Y_COORDS = new double[]{0, -1, 0, 1};

    @Override
    public List<ShapeEntity> getShapesForElement(Particle element) {

        double[] xCoords = getAdjustedXCoordsForParticle(element);
        double[] yCoords = getAdjustedYCoordsForParticle(element);

        return List.of(new ShapeEntity(BasicShape.POLYGON, xCoords, yCoords));
    }

    private double[] getAdjustedXCoordsForParticle(Particle p) {
        double particleX = p.getPosition().getX();
        double radius = p.getRadius();
        double rotationSin = Math.sin(p.getVelocity().angle());
        double rotationCos = Math.cos(p.getVelocity().angle());

        return IntStream.range(0, X_COORDS.length)
                .mapToDouble(i -> particleX + X_COORDS[i] * radius * rotationCos - Y_COORDS[i] * radius * rotationSin)
                .toArray();
    }

    private double[] getAdjustedYCoordsForParticle(Particle p) {
        double particleY = p.getPosition().getY();
        double radius = p.getRadius();
        double rotationSin = Math.sin(p.getVelocity().angle());
        double rotationCos = Math.cos(p.getVelocity().angle());

        return IntStream.range(0, Y_COORDS.length)
                .mapToDouble(i -> particleY + X_COORDS[i] * radius * rotationSin + Y_COORDS[i] * radius * rotationCos)
                .toArray();
    }
}
