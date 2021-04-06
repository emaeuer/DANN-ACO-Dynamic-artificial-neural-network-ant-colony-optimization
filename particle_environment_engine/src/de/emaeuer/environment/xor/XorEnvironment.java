package de.emaeuer.environment.xor;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.AgentController;
import de.emaeuer.environment.configuration.EnvironmentConfiguration;
import de.emaeuer.environment.elements.AbstractElement;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;
import org.apache.commons.math3.linear.ArrayRealVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class XorEnvironment extends AbstractEnvironment  {

    private static record XorTestData(double[] input, double target) {}

    private static final List<XorTestData> TEST_DATA = Arrays.asList(
            new XorTestData(new double[] {0, 0}, 0),
            new XorTestData(new double[] {0, 1}, 1),
            new XorTestData(new double[] {1, 0}, 1),
            new XorTestData(new double[] {1, 1}, 0));

    private List<AgentController> solutionsToEvaluate;

    private boolean restartNecessary = false;

    public XorEnvironment(ConfigurationHandler<EnvironmentConfiguration> configuration) {
        super(null, configuration);
    }

    @Override
    protected void initializeParticles(List<AgentController> controllers) {
        if (this.solutionsToEvaluate == null) {
            this.solutionsToEvaluate = new ArrayList<>();
        }

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

        List<XorTestData> shuffledInput = new ArrayList<>(TEST_DATA);
        shuffledInput.addAll(TEST_DATA);
        shuffledInput.addAll(TEST_DATA);
        shuffledInput.addAll(TEST_DATA);
        shuffledInput.addAll(TEST_DATA);
        shuffledInput.addAll(TEST_DATA);
        Collections.shuffle(shuffledInput);

        // max error is 4 times an error of 2 squared
        double maxError = shuffledInput.size();
        for (AgentController controller : this.solutionsToEvaluate) {
            double rss = 0;
            for (XorTestData testPair : shuffledInput) {
                double[] input;

                // also shuffle input to prevent memorisation (relevant only for 1,0 and 0,1)
                if (Math.random() > 0.5) {
                    input = new double[] {testPair.input()[0], testPair.input()[1]};
                } else {
                    input = new double[] {testPair.input()[1], testPair.input()[0]};
                }

                double result = controller.getAction(input);
                rss += Math.pow(result - testPair.target(), 2);
            }
            controller.setScore((1 - (rss / maxError)) * 100);
        }

        this.restartNecessary = true;
    }

    //    private double calculateFitness() {
//        return 0;
////        double maxSumDiff = ErrorFunction.getInstance().getMaxError(
////                getTargets().length * getTargets()[ 0 ].length,
////                ( maxResponse - minResponse ), SUM_OF_SQUARES );
////        double maxRawFitnessValue = Math.pow( maxSumDiff, 2 ); // 16 ^ 2 = 256
////
////        double sumDiff = ErrorFunction.getInstance().calculateError( getTargets(), responses, false );
////        if ( sumDiff > maxSumDiff )
////            throw new IllegalStateException( "sum diff > max sum diff" );
////        double rawFitnessValue = Math.pow( maxSumDiff - sumDiff, 2 );
////        double skewedFitness = ( rawFitnessValue / maxRawFitnessValue ) * MAX_FITNESS;
////        int result = (int) skewedFitness;
////        return result;
//    }

    @Override
    public void restart() {
        // prepare optimization method for next iteration
        this.restartNecessary = false;
    }

    @Override
    public boolean allAgentsFinished() {
        // initialize already does complete iteration --> immediate restart necessary
        return restartNecessary;
    }
}
