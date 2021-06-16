package de.emaeuer.optimization.neat;

import com.anji.neat.NeatConfiguration;
import com.anji.util.Properties;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.neat.Genotype;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.neat.mapping.ChromosomeSolutionMapper;
import de.emaeuer.optimization.neat.mapping.ChromosomeSolutionMapping;
import de.emaeuer.optimization.neat.mapping.ConfigurationPropertyMapper;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.InvalidConfigurationException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;

public class NeatHandler extends OptimizationMethod {

    private static final Logger LOG = LogManager.getLogger(NeatHandler.class);

    private Genotype population;

    private ChromosomeSolutionMapper mapper;

    private final List<ChromosomeSolutionMapping> solutions = new ArrayList<>();

    public NeatHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        initNeat(configuration);
    }

    private void initNeat(ConfigurationHandler<OptimizationConfiguration> configuration) {
        // maybe necessary to reset (delete files)

        // initialize population randomly
        try {
            Properties props = new Properties(ConfigurationPropertyMapper.mapConfigurationToProperties(configuration));
            NeatConfiguration neatConfig = new NeatConfiguration(props);

            this.population = Genotype.randomInitialGenotype(neatConfig);
            this.mapper = new ChromosomeSolutionMapper(props);
        } catch (InvalidConfigurationException e) {
            LOG.log(Level.WARN, "Failed to initialize neat handler due to an internal neat error", e);
        }
    }

    @Override
    public void resetAndRestart() {
        super.resetAndRestart();
        initNeat(getOptimizationConfiguration());
    }

    @Override
    protected List<? extends Solution> getCurrentSolutions() {
        return this.solutions;
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        return this.solutions
                .stream()
                .mapToDouble(ChromosomeSolutionMapping::getFitness)
                .summaryStatistics();
    }

    @Override
    protected List<? extends Solution> generateSolutions() {
        this.solutions.clear();

        List<Chromosome> entities = this.population.nextIteration();

        double maxFitness = getOptimizationConfiguration().getValue(OptimizationConfiguration.MAX_FITNESS_SCORE, Double.class);

        entities.stream()
                .map(c -> this.mapper.map(c, maxFitness))
                .forEach(this.solutions::add);

        return this.solutions;
    }

    @Override
    public void update() {
        ChromosomeSolutionMapping bestOfIteration = this.solutions
                .stream()
                .max(Comparator.comparingDouble(ChromosomeSolutionMapping::getFitness))
                .orElse(null);

        setCurrentlyBestSolution(bestOfIteration);

        this.population.evolve();

        super.update();
    }
}
