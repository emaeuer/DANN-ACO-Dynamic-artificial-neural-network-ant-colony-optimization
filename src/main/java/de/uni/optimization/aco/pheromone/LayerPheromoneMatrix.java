package de.uni.optimization.aco.pheromone;

import de.uni.optimization.aco.Ant;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LayerPheromoneMatrix {

    private final List<List<ComplexPheromoneValue>> weightMatrix;
    private final List<ComplexPheromoneValue> biasVector;

    public LayerPheromoneMatrix(RealMatrix initialMatrix, RealVector initialBias) {
        this.weightMatrix = LayerPheromoneMatrix.createInitialWeightMatrix(initialMatrix);
        this.biasVector = LayerPheromoneMatrix.createInitialBiasVector(initialBias);
    }

    private static List<List<ComplexPheromoneValue>> createInitialWeightMatrix(RealMatrix matrix) {
        List<List<ComplexPheromoneValue>> listMatrix = new ArrayList<>();
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            List<ComplexPheromoneValue> currentRow = Arrays.stream(matrix.getRow(i))
                    .mapToObj(ComplexPheromoneValue::new)
                    .collect(Collectors.toCollection(ArrayList::new));
            listMatrix.add(currentRow);
        }
        return listMatrix;
    }

    private static List<ComplexPheromoneValue> createInitialBiasVector(RealVector vector) {
        return Arrays.stream(vector.toArray())
                .mapToObj(ComplexPheromoneValue::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void update(Ant.Decision prevDecision, Ant.Decision decision, double quality) {
        int startIndex = prevDecision.neuronID().neuronID();
        int endIndex = decision.neuronID().neuronID();

        this.weightMatrix.get(endIndex).get(startIndex).update(decision.weightValue(), quality);
        this.biasVector.get(endIndex).update(decision.biasValue(), quality);
    }

    public void dissipate() {
        this.weightMatrix.stream()
                .flatMap(List::stream)
                .forEach(ComplexPheromoneValue::dissipate);
        this.biasVector.forEach(ComplexPheromoneValue::dissipate);
    }

    public RealVector getWeightDecisionVector(int index) {
        RealVector vector = new ArrayRealVector(this.weightMatrix.size());

        for (int i = 0; i < this.weightMatrix.size(); i++) {
            vector.setEntry(i, this.weightMatrix.get(i).get(index).getFixed());
        }

        return vector;
    }

    public RealVector getBiasDecisionVector(int index) {
        RealVector vector = new ArrayRealVector(this.biasVector.size());

        for (int i = 0; i < this.biasVector.size(); i++) {
            vector.setEntry(i, this.biasVector.get(i).getFixed());
        }

        return vector;
    }

    public ComplexPheromoneValue getWeightPheromoneValue(int rowIndex, int columnIndex) {
        return this.weightMatrix.get(rowIndex).get(columnIndex);
    }

    public ComplexPheromoneValue getBiasPheromoneValue(int index) {
        return this.biasVector.get(index);
    }
}
