package de.emaeuer.environment.bird;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.GeneralizationHandler;
import de.emaeuer.environment.bird.configuration.FlappyBirdGeneralizationConfiguration;
import de.emaeuer.optimization.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

class FlappyBirdGeneralizationHandler extends GeneralizationHandler<FlappyBirdGeneralizationConfiguration> {

    private int seedIndex = 0;

    private final List<Integer> seeds = new ArrayList<>();


    public FlappyBirdGeneralizationHandler(ConfigurationHandler<FlappyBirdGeneralizationConfiguration> config, RandomUtil rng) {
        super(config, FlappyBirdGeneralizationConfiguration.getKeysForGeneralization());
        initializeSeeds(rng);
    }

    private void initializeSeeds(RandomUtil rng) {
        int numberOfSeeds = getConfig().getValue(FlappyBirdGeneralizationConfiguration.NUMBER_OF_SEEDS, Integer.class);
        IntStream.range(0, numberOfSeeds)
                .mapToObj(i -> rng.getNextInt())
                .forEach(this.seeds::add);
    }

    @Override
    public void next() {
        if (reachedEnd()) {
            return;
        }

        this.seedIndex++;
        if (this.seedIndex < getConfig().getValue(FlappyBirdGeneralizationConfiguration.NUMBER_OF_SEEDS, Integer.class)) {
            return;
        } else {
            this.seedIndex = 0;
        }

        super.next();
    }

    @Override
    public double getNextValue(FlappyBirdGeneralizationConfiguration key) {
        if (key == FlappyBirdGeneralizationConfiguration.NUMBER_OF_SEEDS) {
            return this.seeds.get(this.seedIndex);
        }
        return super.getNextValue(key);
    }

    @Override
    public int getNumberOfGeneralizationIterations() {
        return super.getNumberOfGeneralizationIterations() * this.seeds.size() * getConfig().getValue(FlappyBirdGeneralizationConfiguration.BIRD_START_HEIGHTS, List.class).size();
    }
}
