package de.emaeuer.environment.balance.twodim;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.balance.twodim.configuration.TwoDimensionalCartPoleConfiguration;

import static de.emaeuer.environment.balance.twodim.configuration.TwoDimensionalCartPoleConfiguration.*;

public record GeneralTwoDimensionalCartPoleData(
        double trackLength,
        double poleAngleThreshold,
        double poleLength,
        double poleMass,
        double poleXStartAngle,
        double poleYStartAngle,
        double forceMagnitude,
        double gravity,
        double pivotFriction,
        double cartMass,
        double timeDelta,
        boolean velocityInput,
        boolean positionInput,
        boolean binaryForce) {

    public GeneralTwoDimensionalCartPoleData(ConfigurationHandler<TwoDimensionalCartPoleConfiguration> configuration) {
        this(
                configuration.getValue(TRACK_LENGTH, Double.class),
                configuration.getValue(ANGLE_THRESHOLD, Double.class),
                configuration.getValue(POLE_ONE_LENGTH, Double.class),
                configuration.getValue(POLE_ONE_MASS, Double.class),
                configuration.getValue(POLE_ONE_X_ANGLE, Double.class),
                configuration.getValue(POLE_ONE_Y_ANGLE, Double.class),
                configuration.getValue(FORCE_MAGNITUDE, Double.class),
                configuration.getValue(GRAVITY, Double.class),
                configuration.getValue(PIVOT_FRICTION, Double.class),
                configuration.getValue(CART_MASS, Double.class),
                configuration.getValue(TIME_DELTA, Double.class),
                configuration.getValue(VELOCITY_INPUT, Boolean.class),
                configuration.getValue(POSITION_INPUT, Boolean.class),
                configuration.getValue(BINARY_FORCE, Boolean.class)
        );
    }
}



