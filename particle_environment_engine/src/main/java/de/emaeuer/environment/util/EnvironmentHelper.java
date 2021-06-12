package de.emaeuer.environment.util;

import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.elements.Particle;
import de.emaeuer.environment.math.Vector2D;

import java.util.function.BiConsumer;

public class EnvironmentHelper {

    private EnvironmentHelper() {
    }

    public static final BiConsumer<Particle, AbstractEnvironment> REFLECT_ON_BORDER = (particle, env) -> {
        // particle reaches right border and wants to go further
        if (particle.getPosition().getX() + particle.getRadius() > env.getWidth() && particle.getVelocity().getX() > 0) {
            particle.getPosition().setX(env.getWidth() - particle.getRadius());
            particle.getVelocity().multiply(new Vector2D(-1, 1));
            //  particle reaches left border and wants to go further
        } else if (particle.getPosition().getX() - particle.getRadius() < 0 && particle.getVelocity().getX() < 0) {
            particle.getPosition().setX(particle.getRadius());
            particle.getVelocity().multiply(new Vector2D(-1, 1));
        }

        //  particle reaches bottom border and wants to go further
        if (particle.getPosition().getY() + particle.getRadius() > env.getHeight() && particle.getVelocity().getY() > 0) {
            particle.getPosition().setX(env.getHeight() - particle.getRadius());
            particle.getVelocity().multiply(new Vector2D(1, -1));
            //  particle reaches top border and wants to go further
        } else if (particle.getPosition().getY() - particle.getRadius() < 0 && particle.getVelocity().getY() < 0) {
            particle.getPosition().setX(particle.getRadius());
            particle.getVelocity().multiply(new Vector2D(1, -1));
        }
    };

    public static final BiConsumer<Particle, AbstractEnvironment> GO_TO_OTHER_SIDE = (particle, env) -> {
        // particle reaches right border and wants to go further
        if (particle.getPosition().getX() > env.getWidth()) {
            particle.getPosition().setX(0);
            // particle reaches left border and wants to go further
        } else if (particle.getPosition().getX() < 0) {
            particle.getPosition().setX(env.getWidth());
        }

        // particle reaches bottom border and wants to go further
        if (particle.getPosition().getY() > env.getHeight()) {
            particle.getPosition().setY(0);
            // particle reaches top border and wants to go further
        } else if (particle.getPosition().getY() < 0) {
            particle.getPosition().setY(env.getHeight());
        }
    };

}
