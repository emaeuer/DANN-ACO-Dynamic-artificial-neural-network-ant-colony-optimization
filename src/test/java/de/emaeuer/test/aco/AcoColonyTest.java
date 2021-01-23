package de.emaeuer.test.aco;

import de.emaeuer.optimization.aco.Ant;
import de.emaeuer.ann.NeuralNetwork;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.junit.jupiter.api.Test;

public class AcoColonyTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */



    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testAntColonyIteration() {
//        NeuralNetwork nn = new NeuralNetwork(2, 2, 2, 2);
//
//        AcoHandler handler = new AcoHandler(nn, 1, this::xorFitnessOfAnt);
//        for (int i = 0; i < 50; i++) {
//            handler.nextIteration();
//        }
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

    private double xorFitnessOfAnt(Ant ant) {
        NeuralNetwork nn = ant.getNeuralNetwork();

        double error = 0;
        error += Math.pow(nn.process(new ArrayRealVector(new double[]{0, 0})).getEntry(0), 2);
        error += Math.pow(nn.process(new ArrayRealVector(new double[]{1, 0})).getEntry(0) - 1, 2);
        error += Math.pow(nn.process(new ArrayRealVector(new double[]{0, 1})).getEntry(0) - 1, 2);
        error += Math.pow(nn.process(new ArrayRealVector(new double[]{1, 1})).getEntry(0), 2);

        return 4 - error;
    }

}
