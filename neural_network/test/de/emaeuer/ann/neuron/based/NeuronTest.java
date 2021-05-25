package de.emaeuer.ann.neuron.based;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.impl.neuron.based.Neuron;
import de.emaeuer.ann.impl.neuron.based.NeuronType;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class NeuronTest {

    private static Neuron buildNeuron(NeuronID id, ActivationFunction function, NeuronType type, double bias) {
        return Neuron.build()
                .id(id)
                .activationFunction(function)
                .type(type)
                .bias(bias)
                .finish();
    }

    @Test
    public void testBuild() {
        assertThrows(UnsupportedOperationException.class, () -> buildNeuron(null, ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0), "Can't build neuron because the id is missing");
        assertThrows(UnsupportedOperationException.class, () -> buildNeuron(new NeuronID(0, 0), null, NeuronType.HIDDEN, 0), "Can't build neuron because the activation function is missing");
        assertThrows(UnsupportedOperationException.class, () -> buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, null, 0), "Can't build neuron because the type is missing");

        Neuron neuron = buildNeuron(new NeuronID(0, 0), ActivationFunction.TANH, NeuronType.HIDDEN, 1);
        assertEquals(ActivationFunction.TANH, neuron.getActivationFunction());
        assertEquals(NeuronType.HIDDEN, neuron.getType());
        assertEquals(new NeuronID(0, 0), neuron.getID());
        assertEquals(0, neuron.getActivation());
        assertEquals(1, neuron.getBias());

        Neuron biasNeuron = buildNeuron(new NeuronID(0, 0), ActivationFunction.TANH, NeuronType.BIAS, 1);
        assertEquals(ActivationFunction.IDENTITY, biasNeuron.getActivationFunction());
        assertEquals(NeuronType.BIAS, biasNeuron.getType());
        assertEquals(new NeuronID(0, 0), biasNeuron.getID());
        assertEquals(1, biasNeuron.getActivation());
        assertEquals(0, biasNeuron.getBias());
    }

    @Test
    public void testAddConnection() {
        // test invalid connection to bias neuron
        Neuron startA = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        Neuron endA = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.BIAS, 0);
        assertThrows(UnsupportedOperationException.class, () -> endA.modify().addInput(startA, 0), "A bias neuron can't have incoming connections");
        checkConnections(startA, new Neuron[] {}, new Neuron[] {});
        checkConnections(endA, new Neuron[] {}, new Neuron[] {});

        // test invalid connection to input neuron
        Neuron startB = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        Neuron endB = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.BIAS, 0);
        assertThrows(UnsupportedOperationException.class, () -> endB.modify().addInput(startB, 0), "An input neuron can't have incoming connections");
        checkConnections(startB, new Neuron[] {}, new Neuron[] {});
        checkConnections(endB, new Neuron[] {}, new Neuron[] {});

        // test valid connection
        Neuron startC = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.BIAS, 0);
        Neuron endC = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        endC.modify().addInput(startC, 1);
        checkConnections(startC, new Neuron[] {}, new Neuron[] {endC});
        checkConnections(endC, new Neuron[] {startC}, new Neuron[] {});
        assertEquals(1, endC.getWeightOfInput(startC));

        // test direct recurrent connection
        Neuron neuron = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.OUTPUT, 0);
        neuron.modify().addInput(neuron, 1);
        checkConnections(neuron, new Neuron[] {neuron}, new Neuron[] {neuron});
        assertEquals(1, neuron.getWeightOfInput(neuron));
    }

    @Test
    public void testChangeConnection() {
        Neuron startA = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        Neuron endA = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        endA.modify().addInput(startA, 1).changeWeightOfConnection(startA, -0.5);
        assertEquals(-0.5, endA.getWeightOfInput(startA));

        Neuron startB = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        Neuron endB = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        assertThrows(IllegalArgumentException.class, () -> endB.modify().changeWeightOfConnection(startB, -0.5), "Can't change weight of non existing connection");
    }

    @Test
    public void testRemoveConnection() {
        Neuron startA = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        Neuron endA = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        endA.modify().addInput(startA, 1).removeInput(startA);
        checkConnections(startA, new Neuron[] {}, new Neuron[] {});
        checkConnections(endA, new Neuron[] {}, new Neuron[] {});
        assertEquals(0, endA.getWeightOfInput(endA));

        Neuron startB = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        Neuron endB = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        endB.modify().removeInput(startB);
        checkConnections(startB, new Neuron[] {}, new Neuron[] {});
        checkConnections(endB, new Neuron[] {}, new Neuron[] {});
        assertEquals(0, endB.getWeightOfInput(endB));
    }

    @Test
    public void testActivationWithoutInput() {
        Neuron neuron = buildNeuron(new NeuronID(0, 0), ActivationFunction.SIGMOID, NeuronType.HIDDEN, 2);
        neuron.activate();
        assertEquals(ActivationFunction.SIGMOID.apply(2), neuron.getActivation());
        neuron.activate();
        assertEquals(ActivationFunction.SIGMOID.apply(2), neuron.getActivation());
    }

    @Test
    public void testActivationWithSingleConnection() {
        Neuron start = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.BIAS, 1);
        Neuron end = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.HIDDEN, -2);
        end.modify().addInput(start, -1);

        // test no dependencies between iterations
        for (int i = 0; i < 100; i++) {
            start.activate();
            end.activate();

            assertEquals(1, start.getActivation());
            assertEquals(-3, end.getActivation());
        }
    }

    @Test
    public void testActivationWithMultipleConnections() {
        Neuron startA = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.BIAS, 1);
        Neuron startB = buildNeuron(new NeuronID(0, 1), ActivationFunction.IDENTITY, NeuronType.INPUT, 0);
        Neuron startC = buildNeuron(new NeuronID(0, 2), ActivationFunction.IDENTITY, NeuronType.HIDDEN, -1);
        Neuron end = buildNeuron(new NeuronID(0, 3), ActivationFunction.IDENTITY, NeuronType.OUTPUT, -2);
        end.modify().addInput(startA, 2)
                .addInput(startB, 1)
                .addInput(startC, -2);

        // test no dependencies between iterations
        for (int i = 0; i < 100; i++) {
            startA.activate();
            startB.activate(i); // external input
            startC.activate();
            end.activate();

            assertEquals(1, startA.getActivation());
            assertEquals(i, startB.getActivation());
            assertEquals(-1, startC.getActivation());
            assertEquals(2 + i, end.getActivation());
        }
    }

    @Test
    public void testActivationWithRecurrentConnection() {
        Neuron start = buildNeuron(new NeuronID(0, 0), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 1);
        Neuron end = buildNeuron(new NeuronID(0, 3), ActivationFunction.IDENTITY, NeuronType.HIDDEN, 0);
        end.modify().addInput(start, -1);
        start.modify().addInput(end, 1);

        // test no dependencies between iterations
        for (int i = 0; i < 100; i++) {
            start.activate();
            end.activate();

            assertEquals(i % 2 == 0 ? 1 : 0, start.getActivation());
            assertEquals(i % 2 == 0 ? -1 : 0, end.getActivation());
        }
    }

    private void checkConnections(Neuron neuron, Neuron[] incoming, Neuron[] outgoing) {
        assertArrayEquals(incoming, neuron.getIncomingConnections().toArray(Neuron[]::new));
        assertArrayEquals(outgoing, neuron.getOutgoingConnections().toArray(Neuron[]::new));
    }

}
