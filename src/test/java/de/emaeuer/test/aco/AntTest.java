//package de.uni.test.aco;
//
//import de.uni.optimization.aco.Ant;
//import de.uni.optimization.aco.pheromone.CompositePheromoneMatrix;
//import de.uni.optimization.aco.pheromone.LayerPheromoneMatrix;
//import de.uni.ann.NeuralNetwork;
//import de.uni.ann.Neuron;
//import org.apache.commons.math3.linear.RealMatrix;
//import org.junit.jupiter.api.Test;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//public class AntTest {
//
//   /*
//     ##########################################################
//     ################# Data creation Methods ##################
//     ##########################################################
//    */
//
//    private CompositePheromoneMatrix generateFinalizedPheromoneMatrix(NeuralNetwork nn, boolean inhibitory) {
//        CompositePheromoneMatrix pheromone = CompositePheromoneMatrix.buildForNeuralNetwork(nn);
//
//        for (LayerPheromoneMatrix pheromoneMatrix : pheromone) {
//            pheromoneMatrix.getWeightExcitatory().setEntry(0, 0, 0);
//            pheromoneMatrix.getWeightExcitatory().setEntry(0, 1, 0);
//            pheromoneMatrix.getWeightExcitatory().setEntry(1, 0, 0);
//            pheromoneMatrix.getWeightExcitatory().setEntry(1, 1, inhibitory ? 0 : 1);
//            pheromoneMatrix.getWeightInhibitory().setEntry(0, 0, 0);
//            pheromoneMatrix.getWeightInhibitory().setEntry(0, 1, 0);
//            pheromoneMatrix.getWeightInhibitory().setEntry(1, 0, 0);
//            pheromoneMatrix.getWeightInhibitory().setEntry(1, 1, inhibitory ? 1 : 0);
//        }
//        return pheromone;
//    }
//
//    /*
//     ##########################################################
//     ##################### Test Methods #######################
//     ##########################################################
//    */
//
////    @Test
////    public void testWalkInitializedPheromone() {
////        NeuralNetwork nn = new NeuralNetwork(2, 2, 2, 2);
////        CompositePheromoneMatrix pheromone = CompositePheromoneMatrix.buildForNeuralNetwork(nn);
////
////        Ant ant = new Ant(nn, new Neuron.NeuronID(0,0), pheromone);
////        ant.walk(1);
////        List<Ant.Decision> solution = ant.getSolution();
////
////        assertEquals(nn.getNumberOfLayers(), solution.size());
////
////        for (int i = 0, j = 1; j < solution.size(); i++, j++) {
////            Neuron.NeuronID startID = solution.get(i).neuronID();
////            Neuron.NeuronID endID = solution.get(j).neuronID();
////
////            RealMatrix weights = nn.getLayer(endID.layerID()).getWeights();
////            // check solution was updated
////            assertEquals(solution.get(j).weightInhibitory() ? -1 : 1, weights.getEntry(endID.neuronID(), startID.neuronID()));
////            // check nothing else was updated
////            weights.setEntry(endID.neuronID(), startID.neuronID(), 0);
////            assertTrue(Arrays.stream(weights.getData())
////                    .flatMapToDouble(Arrays::stream)
////                    .noneMatch(d -> d != 0));
////        }
////    }
//
//    // finalized means that one connection has all the pheromone
//    // --> check that only the connection with pheromone is chosen
////    @Test
////    public void testWalkFinalizedPheromone() {
////        for (int i = 0; i < 2; i++) {
////            NeuralNetwork nn = new NeuralNetwork(2, 2, 2, 2);
////            CompositePheromoneMatrix pheromone = generateFinalizedPheromoneMatrix(nn, i == 0); // test for weightInhibitory and for excitatory
////
////            Ant ant = new Ant(nn, new Neuron.NeuronID(0,1), pheromone);
////            ant.walk(1);
////            List<Ant.Decision> solution = ant.getSolution();
////
////            assertEquals(nn.getNumberOfLayers(), solution.size());
////            solution.forEach(s -> assertEquals(1, s.neuronID().neuronID()));
////        }
////    }
//
//    /*
//     ##########################################################
//     #################### Helper Methods ######################
//     ##########################################################
//    */
//
//}
