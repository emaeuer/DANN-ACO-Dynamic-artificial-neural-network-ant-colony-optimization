package de.emaeuer.environment.util;

import de.emaeuer.environment.force.Force;
import de.emaeuer.environment.math.Vector2D;

public class ForceHelper {

    private ForceHelper() {}

    public static Force createBasicForce(Vector2D force) {
        return p -> p.applyForce(force);
    }

    public static Force createSteeringForce(Vector2D target, double arrivingBehaviour) {
        return createSteeringForce(target, 1.0, arrivingBehaviour);
    }

    /**
     * Create steering force without arriving behaviour
     */
    public static Force createSteeringForce(Vector2D target) {
        return createSteeringForce(target, 1.0, 0);
    }

    public static Force createSteeringForce(Vector2D target, double steeringFactor, double arrivingBehaviour) {
        return p -> {
            Vector2D desiredVelocity = Vector2D.subtract(target, p.getPosition());
            double distance = desiredVelocity.magnitude();

            desiredVelocity.normalize().multiply(p.getMaxVelocity());

            if (distance < arrivingBehaviour) {
                desiredVelocity.multiply(distance / arrivingBehaviour);
            }

            desiredVelocity.limit(p.getMaxVelocity() * steeringFactor);

            p.applyForce(Vector2D.subtract(desiredVelocity, p.getVelocity()).multiply(p.getMass()));
        };
    }

}
