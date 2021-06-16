package de.emaeuer.environment.xor;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

public class XorEnvironment extends AbstractEnvironment {

    private static final Logger LOG = LogManager.getLogger(XorEnvironment.class);

    private static record XorTestData(double[] input, double target) {
    }

    private static final List<XorTestData> TEST_DATA = Arrays.asList(
            new XorTestData(new double[]{0, 0}, 0),
            new XorTestData(new double[]{0, 1}, 1),
            new XorTestData(new double[]{1, 0}, 1),
            new XorTestData(new double[]{1, 1}, 0));

    private final List<AgentController> solutionsToEvaluate = new ArrayList<>();

    private boolean restartNecessary = false;

    private final double targetPrecision;

    private final boolean shuffleInput;

    private final int numberOfEvaluations;

    public XorEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);
        ConfigurationHandler<XORConfiguration> xorConfiguration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, XORConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);

        this.targetPrecision = xorConfiguration.getValue(XORConfiguration.TARGET_RANGE, Double.class);
        this.shuffleInput = xorConfiguration.getValue(XORConfiguration.SHUFFLE_DATA_SET, Boolean.class);
        this.numberOfEvaluations = xorConfiguration.getValue(XORConfiguration.DATA_SET_SIZE_FACTOR, Integer.class);
    }

    @Override
    protected void initializeParticles(List<AgentController> controllers) {
        this.solutionsToEvaluate.clear();
        this.solutionsToEvaluate.addAll(controllers);
    }

    private boolean oddIteration = true;

    @Override
    public void step() {
        super.step();

        // only do something every second update or the gui will not update because restart necessary is always true
        this.oddIteration = !this.oddIteration;
        if (oddIteration) {
            return;
        }

        List<XorTestData> input = new ArrayList<>();

        IntStream.range(0, this.numberOfEvaluations)
                .forEach(i -> input.addAll(TEST_DATA));

        for (AgentController controller : this.solutionsToEvaluate) {
            if (this.shuffleInput) {
                getRNG().shuffleCollection(input);
            }

            double maxError = (1 - this.targetPrecision) * input.size();
            double error = 0;

            for (XorTestData stimulus : input) {
                double activation = controller.getAction(stimulus.input())[0];
                activation = adjustActivation(activation, controller);
                error += Math.max(Math.abs(activation - stimulus.target) - this.targetPrecision, 0);
            }

            if (error > maxError) {
                LOG.warn("The calculated error is larger than the maximal error");
                error = maxError;
            }

            controller.setScore(Math.pow(maxError - error, 2) / Math.pow(maxError, 2));
        }

        this.restartNecessary = true;
    }

    private double adjustActivation(double activation, AgentController controller) {
        // 10 and -10 are arbitrary bounds in case of unlimited values
        double maxActivation = Math.min(controller.getMaxAction(), 10);
        double minActivation = Math.max(controller.getMinAction(), -10);
        double activationRange = maxActivation - minActivation;

        // adjust to [0:1]
        return (activation - minActivation) / activationRange;
    }

    @Override
    public void restart() {
        super.restart();
        // prepare optimization method for next iteration
        this.restartNecessary = false;
        getRNG().reset();
    }

    @Override
    public boolean allAgentsFinished() {
        // initialize already does complete iteration --> immediate restart necessary
        return restartNecessary;
    }
}
