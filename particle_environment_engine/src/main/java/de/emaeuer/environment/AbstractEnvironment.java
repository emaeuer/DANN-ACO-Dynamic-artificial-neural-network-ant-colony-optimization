package de.emaeuer.environment;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.configuration.GeneralizationConfiguration;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.*;
import java.util.function.BiConsumer;

public abstract class AbstractEnvironment<T extends Enum<T> & DefaultConfiguration<T> & GeneralizationConfiguration<T>> {

    private final double width = 800;
    private final double height = 800;

    private final List<AgentController> agentControllers = Collections.synchronizedList(new ArrayList<>());
    private final List<AbstractElement> agentsToDraw = Collections.synchronizedList(new ArrayList<>());

    private final Map<AgentController, AgentController> originControllers = new IdentityHashMap<>();

    private final BiConsumer<AbstractElement, AbstractEnvironment<T>> borderStrategy;

    private double maxStepNumber;
    private double maxGeneralizationStepNumber;
    private double currentGeneralizationCapability;

    private boolean controllerFinishedWithoutDying = false;
    private boolean finishedGeneralization = false;
    private boolean testingGeneralization = false;

    private GeneralizationHandler<T> generalizationHandler;

    private final RandomUtil rng;

    public AbstractEnvironment(BiConsumer<AbstractElement, AbstractEnvironment<T>> borderStrategy, ConfigurationHandler<EnvironmentConfiguration> configuration) {
        this.borderStrategy = borderStrategy;
        this.rng = new RandomUtil(configuration.getValue(EnvironmentConfiguration.SEED, Integer.class));

        configuration.logConfiguration();

        initialize(configuration);
    }

    protected void initialize(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        this.maxStepNumber = configuration.getValue(EnvironmentConfiguration.MAX_STEP_NUMBER, Double.class);
        this.maxGeneralizationStepNumber = configuration.getValue(EnvironmentConfiguration.GENERALIZATION_MAX_STEP_NUMBER, Double.class);
    }

    public void setControllers(List<AgentController> controllers) {
        restart();
        this.agentControllers.addAll(controllers);
        initializeParticles(controllers);
    }

    public void step() {
        this.agentsToDraw.stream()
                .peek(AbstractElement::step)
                .forEach(this::checkBorderCase);
    }

    public void testGeneralization() {
        AgentController best = getAgents().stream()
                .max(Comparator.comparingDouble(AgentController::getScore))
                .orElse(null);
        getAgents().clear();

        this.generalizationHandler = getNewGeneralizationHandler();

        if (this.generalizationHandler == null || best == null) {
            setTestingGeneralization(false);
            setFinishedGeneralization(true);
        } else {
            getAgents().add(best);
            setTestingGeneralization(true);
            setFinishedGeneralization(false);
        }

        nextGeneralizationIteration();
    }

    protected abstract GeneralizationHandler<T> getNewGeneralizationHandler();

    public void nextGeneralizationIteration() {
        this.rng.reset();
        this.agentsToDraw.clear();

        if (finishedGeneralization() || getGeneralizationHandler().getNumberOfGeneralizationIterations() == 0 || getGeneralizationHandler().reachedEnd()) {
            setFinishedGeneralization(true);
            setTestingGeneralization(false);
        }
    }

    protected abstract void initializeParticles(List<AgentController> controllers);

    public void restart() {
        this.rng.reset();
        setControllerFinishedWithoutDying(false);
        setFinishedGeneralization(false);
        setCurrentGeneralizationCapability(0);
        this.agentsToDraw.clear();
        this.agentControllers.clear();
    }

    protected void checkBorderCase(AbstractElement particle) {
        if (this.borderStrategy != null) {
            this.borderStrategy.accept(particle, this);
        }
    }

    public List<AbstractElement> getAdditionalEnvironmentElements() {
        return Collections.synchronizedList(new ArrayList<>());
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public List<AgentController> getAgents() {
        return agentControllers;
    }

    public List<AbstractElement> getAgentsToDraw() {
        return agentsToDraw;
    }

    public abstract boolean environmentFinished();

    public double getMaxStepNumber() {
        return maxStepNumber;
    }

    public double getMaxGeneralizationStepNumber() {
        return maxGeneralizationStepNumber;
    }

    protected RandomUtil getRNG() {
        return this.rng;
    }

    public boolean controllerFinishedWithoutDying() {
        return this.controllerFinishedWithoutDying;
    }

    protected void setControllerFinishedWithoutDying(boolean controllerFinishedWithoutDying) {
        this.controllerFinishedWithoutDying = controllerFinishedWithoutDying;
    }

    public boolean finishedGeneralization() {
        return this.finishedGeneralization;
    }

    protected void setFinishedGeneralization(boolean finishedGeneralization) {
        this.finishedGeneralization = finishedGeneralization;
    }

    public boolean isTestingGeneralization() {
        return testingGeneralization;
    }

    protected void setTestingGeneralization(boolean testingGeneralization) {
        this.testingGeneralization = testingGeneralization;
    }

    public double getCurrentGeneralizationProgress() {
        return this.currentGeneralizationCapability;
    }

    protected void setCurrentGeneralizationCapability(double currentGeneralizationCapability) {
        this.currentGeneralizationCapability = currentGeneralizationCapability;
    }

    protected Map<AgentController, AgentController> getOriginControllers() {
        return originControllers;
    }

    public GeneralizationHandler<T> getGeneralizationHandler() {
        return generalizationHandler;
    }
}
