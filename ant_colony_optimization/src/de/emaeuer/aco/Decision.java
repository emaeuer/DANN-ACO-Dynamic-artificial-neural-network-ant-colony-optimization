package de.emaeuer.aco;

import de.emaeuer.ann.NeuronID;

public record Decision(NeuronID neuronID, double weightValue, double biasValue) {
}
