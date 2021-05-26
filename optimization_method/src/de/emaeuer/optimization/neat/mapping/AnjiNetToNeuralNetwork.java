package de.emaeuer.optimization.neat.mapping;

import com.anji.integration.AnjiActivator;
import com.anji.nn.*;
import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.impl.neuron.based.Neuron;
import de.emaeuer.ann.impl.neuron.based.NeuronBasedNeuralNetworkBuilder;
import de.emaeuer.ann.impl.neuron.based.NeuronType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AnjiNetToNeuralNetwork {

    private AnjiNetToNeuralNetwork() {}

    public static NeuralNetwork mapToNeuralNetwork(AnjiActivator activator) {
        AnjiNet net = activator.getNeuralNetwork();

        Map<Long, Neuron> idNeuronMapping = new HashMap<>();

        int counter = 0;
        for (com.anji.nn.Neuron neuron : net.getInNeurons()) {
            Neuron representation = Neuron.build()
                    .id(new NeuronID(0, counter++))
                    .bias(0)
                    .type(NeuronType.INPUT)
                    .activationFunction(mapActivationFunction(neuron.getFunc()))
                    .finish();
            idNeuronMapping.put(neuron.getId(), representation);
        }

        counter = 0;
        for (com.anji.nn.Neuron neuron : net.getOutNeurons()) {
            Neuron representation = Neuron.build()
                    .id(new NeuronID(2, counter++))
                    .bias(0)
                    .type(NeuronType.OUTPUT)
                    .activationFunction(mapActivationFunction(neuron.getFunc()))
                    .finish();
            idNeuronMapping.put(neuron.getId(), representation);
        }

        counter = 0;
        for (com.anji.nn.Neuron neuron : net.getAllNeurons()) {
            if (idNeuronMapping.containsKey(neuron.getId())) {
                continue;
            }

            Neuron representation = Neuron.build()
                    .id(new NeuronID(1, counter++))
                    .bias(0)
                    .type(NeuronType.HIDDEN)
                    .activationFunction(mapActivationFunction(neuron.getFunc()))
                    .finish();
            idNeuronMapping.put(neuron.getId(), representation);
        }

        for (com.anji.nn.Neuron neuron : net.getAllNeurons()) {
            Neuron end = idNeuronMapping.get(neuron.getId());
            for (Connection c : neuron.getIncomingConnections()) {
                if (c instanceof NeuronConnection connection) {
                    Neuron start = idNeuronMapping.get(connection.getIncomingNode().getId());
                    end.modify().addInput(start, connection.getWeight());
                }
            }
        }

        return NeuronBasedNeuralNetworkBuilder.buildFromNeuronCollection(new ArrayList<>(idNeuronMapping.values()));
    }

    private static ActivationFunction mapActivationFunction(com.anji.nn.ActivationFunction func) {
        if (func instanceof TanhActivationFunction) {
            return ActivationFunction.TANH;
        } else if (func instanceof SigmoidActivationFunction) {
            return ActivationFunction.SIGMOID;
        } else if (func instanceof LinearActivationFunction) {
            return ActivationFunction.IDENTITY;
        } else if (func instanceof ClampedLinearActivationFunction) {
            return ActivationFunction.LINEAR_UNTIL_SATURATION;
        } else {
            return null;
        }
    }

}
