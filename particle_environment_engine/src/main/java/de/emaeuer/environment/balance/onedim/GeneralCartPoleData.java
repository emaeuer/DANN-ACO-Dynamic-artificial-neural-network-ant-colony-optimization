package de.emaeuer.environment.balance.onedim;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.balance.onedim.configuration.CartPoleConfiguration;

import static de.emaeuer.environment.balance.onedim.configuration.CartPoleConfiguration.*;

public record GeneralCartPoleData(
        double trackLength,
        double poleAngleThreshold,
        double poleOneLength,
        double poleOneMass,
        double poleOneStartAngle,
        double poleTwoLength,
        double poleTwoMass,
        double poleTwoStartAngle,
        double forceMagnitude,
        double gravity,
        double pivotFriction,
        double cartMass,
        double timeDelta,
        boolean velocityInput,
        boolean randomStartAngle,
        boolean twoPoles,
        boolean penalizeOscillation,
        boolean positionInput,
        boolean binaryForce) {

    public GeneralCartPoleData(ConfigurationHandler<CartPoleConfiguration> configuration) {
        this(
                configuration.getValue(TRACK_LENGTH, Double.class),
                configuration.getValue(ANGLE_THRESHOLD, Double.class),
                configuration.getValue(POLE_ONE_LENGTH, Double.class),
                configuration.getValue(POLE_ONE_MASS, Double.class),
                configuration.getValue(POLE_ONE_ANGLE, Double.class),
                configuration.getValue(POLE_TWO_LENGTH, Double.class),
                configuration.getValue(POLE_TWO_MASS, Double.class),
                configuration.getValue(POLE_TWO_ANGLE, Double.class),
                configuration.getValue(FORCE_MAGNITUDE, Double.class),
                configuration.getValue(GRAVITY, Double.class),
                configuration.getValue(PIVOT_FRICTION, Double.class),
                configuration.getValue(CART_MASS, Double.class),
                configuration.getValue(TIME_DELTA, Double.class),
                configuration.getValue(VELOCITY_INPUT, Boolean.class),
                configuration.getValue(RANDOM_START_ANGLE, Boolean.class),
                configuration.getValue(USE_TWO_POLES, Boolean.class),
                configuration.getValue(PENALIZE_OSCILLATION, Boolean.class),
                configuration.getValue(POSITION_INPUT, Boolean.class),
                configuration.getValue(BINARY_FORCE, Boolean.class)
        );
    }
}



