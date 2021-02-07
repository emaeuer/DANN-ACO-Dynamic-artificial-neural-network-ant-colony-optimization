package de.emaeuer.optimization;

import java.util.DoubleSummaryStatistics;
import java.util.List;

public interface OptimizationMethod {

    public List<Solution> generateSolutions();

    public void update();

    public List<DoubleSummaryStatistics> getStatisticsOfIteration();

    public int getIterationCount();
}
