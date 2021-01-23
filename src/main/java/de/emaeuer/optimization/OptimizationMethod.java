package de.emaeuer.optimization;

import java.util.List;

public interface OptimizationMethod {

    public List<Solution> generateSolutions();

    public void update();
}
