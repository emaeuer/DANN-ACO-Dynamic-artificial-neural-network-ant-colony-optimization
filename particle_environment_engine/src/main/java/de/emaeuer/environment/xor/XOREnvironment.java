package de.emaeuer.environment.xor;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.GeneralizationHandler;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

public class XOREnvironment extends AbstractEnvironment<XORGeneralizationConfiguration> {

    private static final Logger LOG = LogManager.getLogger(XOREnvironment.class);

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

    private final int dataSetSize;
    private final int generalizationDataSetSize;

    public XOREnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);
        ConfigurationHandler<XORConfiguration> xorConfiguration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, XORConfiguration.class, EnvironmentConfiguration.ENVIRONMENT_IMPLEMENTATION);

        if (configuration.getValue(EnvironmentConfiguration.TEST_GENERALIZATION, Boolean.class)) {
            ConfigurationHandler<XORGeneralizationConfiguration> generalizationConfiguration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, XORGeneralizationConfiguration.class, EnvironmentConfiguration.GENERALIZATION_IMPLEMENTATION);
            this.generalizationDataSetSize = generalizationConfiguration.getValue(XORGeneralizationConfiguration.GENERALIZATION_DATA_SET_SIZE, Integer.class);
        } else {
            this.generalizationDataSetSize = 0;
        }

        this.targetPrecision = xorConfiguration.getValue(XORConfiguration.TARGET_RANGE, Double.class);
        this.shuffleInput = xorConfiguration.getValue(XORConfiguration.SHUFFLE_DATA_SET, Boolean.class);
        this.dataSetSize = xorConfiguration.getValue(XORConfiguration.DATA_SET_SIZE, Integer.class);
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

        List<XorTestData> dataSet = createDataSet(this.dataSetSize, this.shuffleInput);
        this.solutionsToEvaluate.stream()
                .peek(c -> evaluateController(c, dataSet))
                .forEach(this::checkController);

        this.restartNecessary = true;
    }

    private void checkController(AgentController controller) {
        if (controller.getScore() >= 1) {
            setControllerFinishedWithoutDying(true);
        }
    }

    private List<XorTestData> createDataSet(int dataSetSize, boolean shuffle) {
        List<XorTestData> dataSet = new ArrayList<>();

        int remainder = dataSetSize % 4;
        int inputNumber = dataSetSize / 4;
        inputNumber += remainder == 0 ? 0 : 1;

        IntStream.range(0, inputNumber)
                .forEach(i -> dataSet.addAll(TEST_DATA));

        if (shuffle) {
            getRNG().shuffleCollection(dataSet);
        }

        for (int i = 0; i < remainder; i++) {
            dataSet.remove(dataSetSize - 1);
        }

        return dataSet;
    }

    private void evaluateController(AgentController controller, List<XorTestData> dataSet) {
        double maxError = (1 - this.targetPrecision) * dataSet.size();
        double error = 0;

        for (XorTestData stimulus : dataSet) {
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

    public void testGeneralization() {
        List<AgentController> controllersToRemove = getAgents().stream()
                .filter(c -> c.getScore() < 1)
                .toList();

        getAgents().removeAll(controllersToRemove);

        setTestingGeneralization(true);

        if (getAgents().isEmpty()) {
            setTestingGeneralization(false);
            setFinishedGeneralization(true);
        } else {
            setTestingGeneralization(true);
            setFinishedGeneralization(false);
        }

        nextGeneralizationIteration();
    }

    @Override
    protected GeneralizationHandler<XORGeneralizationConfiguration> getNewGeneralizationHandler() {
        return null;
    }

    @Override
    public void nextGeneralizationIteration() {
        List<XorTestData> dataSet = createDataSet(this.generalizationDataSetSize, true);

        for (AgentController agent : getAgents()) {
            AgentController copy = agent.copy();
            evaluateController(copy, dataSet);
            agent.setGeneralizationCapability(copy.getScore());
        }

        setTestingGeneralization(false);
        setFinishedGeneralization(true);
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
    public boolean environmentFinished() {
        // initialize already does complete iteration --> immediate restart necessary
        return restartNecessary;
    }
}
