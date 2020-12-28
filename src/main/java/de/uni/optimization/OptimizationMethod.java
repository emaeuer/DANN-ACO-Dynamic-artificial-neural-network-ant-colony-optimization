package de.uni.optimization;

import de.uni.optimization.aco.Ant;

import java.util.List;

public interface OptimizationMethod {

    public List<Solution> generateSolutions();

    public void update();
}
