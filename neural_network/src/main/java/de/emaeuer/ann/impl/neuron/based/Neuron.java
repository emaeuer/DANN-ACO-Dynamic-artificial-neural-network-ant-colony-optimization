package de.emaeuer.ann.impl.neuron.based;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.NeuronID;

import java.util.*;

public class Neuron {

    public static class NeuronBuilder {

        private int necessaryModificationFlag = 0b111;
        private final Neuron neuron = new Neuron();

        private NeuronBuilder() {}

        public NeuronBuilder activationFunction(ActivationFunction function) {
            if (function == null) {
                return this;
            }

            this.neuron.activationFunction = function;
            this.necessaryModificationFlag &= 0b110;
            return this;
        }

        public NeuronBuilder type(NeuronType type) {
            if (type == null) {
                return this;
            }

            this.neuron.type = type;
            this.necessaryModificationFlag &= 0b011;
            return this;
        }

        public NeuronBuilder id(NeuronID id) {
            if (id == null) {
                return this;
            }

            this.neuron.recurrentID = id.getNeuronIndex();
            this.neuron.id = id;
            this.necessaryModificationFlag &= 0b101;
            return this;
        }

        public NeuronBuilder bias(double bias) {
            this.neuron.bias = bias;
            return this;
        }

        public Neuron finish() {
            if (this.neuron.type == NeuronType.BIAS) {
                activationFunction(ActivationFunction.IDENTITY);
                bias(0);
                this.neuron.lastActivation = 1;
            }

            switch (Integer.highestOneBit(this.necessaryModificationFlag)) {
                case 1 -> throw new UnsupportedOperationException("Can't build neuron because the activation function is missing");
                case 2 -> throw new UnsupportedOperationException("Can't build neuron because the id is missing");
                case 4 -> throw new UnsupportedOperationException("Can't build neuron because the type is missing");
            }

            return this.neuron;
        }

    }

    public static class NeuronModifier {

        private final Neuron neuron;

        private NeuronModifier(Neuron neuron) {
            this.neuron = neuron;
        }

        public NeuronModifier bias(double bias) {
            neuron.bias = bias;
            return this;
        }

        public NeuronModifier addInput(Neuron source, double weight) {
            validate();
            this.neuron.inputs.put(source, weight);
            source.outputs.add(this.neuron);

            return this;
        }

        public NeuronModifier removeInput(Neuron source) {
            this.neuron.inputs.remove(source);
            source.outputs.remove(this.neuron);

            return this;
        }

        public NeuronModifier changeWeightOfConnection(Neuron source, double weight) {
            if (!this.neuron.inputs.containsKey(source) || !source.outputs.contains(this.neuron)) {
                throw new IllegalArgumentException("Can't change weight of non existing connection");
            }

            this.neuron.inputs.put(source, weight);

            return this;
        }

        private void validate() {
            if (this.neuron.type == NeuronType.BIAS) {
                throw new UnsupportedOperationException("A bias neuron can't have incoming connections");
            }
        }

        public NeuronModifier disconnectAll() {
            List<Neuron> outgoingCopy = new ArrayList<>(this.neuron.getOutgoingConnections());
            List<Neuron> incomingCopy = new ArrayList<>(this.neuron.getIncomingConnections());

            outgoingCopy.forEach(t -> t.modify().removeInput(this.neuron));
            incomingCopy.forEach(s -> this.neuron.modify().removeInput(s));

            return this;
        }
    }

    private NeuronID id;
    private int recurrentID;

    private final Map<Neuron, Double> inputs = new HashMap<>();
    private final Set<Neuron> outputs = new HashSet<>();

    private NeuronType type;

    private ActivationFunction activationFunction;

    private double lastActivation = 0;
    private double bias = 0;

    private boolean wasAlreadyActivated = false;

    private final NeuronModifier modifier = new NeuronModifier(this);

    private Neuron() {}

    public static NeuronBuilder build() {
        return new NeuronBuilder();
    }

    public NeuronModifier modify() {
        return this.modifier;
    }

    public void activate(double activation) {
        if (this.type == NeuronType.INPUT) {
            this.lastActivation = this.activationFunction.apply(activation + this.bias);
        } else {
            throw new UnsupportedOperationException("Can't set external value of hidden, bias or output neuron");
        }
    }

    public double activate() {
        // activation of input and bias neurons is changed externally
        if (this.type == NeuronType.BIAS || this.type == NeuronType.INPUT || this.wasAlreadyActivated) {
            return this.lastActivation;
        }

        this.wasAlreadyActivated = true;

        double weightedSum = this.inputs.entrySet()
                .stream()
                .map(this::getInputValue)
                .mapToDouble(Double::doubleValue)
                .sum();

        weightedSum += this.bias;

        this.lastActivation = this.activationFunction.apply(weightedSum);
        return this.lastActivation;
    }

    private double getInputValue(Map.Entry<Neuron, Double> input) {
        double weight = input.getValue();
        double activation = input.getKey().activate();

        return activation * weight;
    }

    public Neuron copyWithoutConnections() {
        Neuron copy = new Neuron();

        copy.activationFunction = this.activationFunction;
        copy.type = this.type;
        copy.id = new NeuronID(this.id.getLayerIndex(), this.id.getNeuronIndex());
        copy.bias = this.bias;
        copy.recurrentID = this.recurrentID;

        if (this.type == NeuronType.BIAS) {
            copy.lastActivation = this.lastActivation;
        }

        return copy;
    }

    public double getActivation() {
        return this.lastActivation;
    }

    public NeuronID getID() {
        return this.id;
    }

    public double getBias() {
        return this.bias;
    }

    public double getWeightOfInput(Neuron neuron) {
        return this.inputs.getOrDefault(neuron, 0.0);
    }

    public boolean hasConnectionTo(Neuron end) {
        return this.outputs.contains(end);
    }

    public Set<Neuron> getIncomingConnections() {
        return inputs.keySet();
    }

    public Set<Neuron> getOutgoingConnections() {
        return outputs;
    }

    public ActivationFunction getActivationFunction() {
        return this.activationFunction;
    }

    public NeuronType getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) return false;

        Neuron neuron = (Neuron) o;

        return Objects.equals(id, neuron.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public int getRecurrentID() {
        return recurrentID;
    }

    public void setRecurrentID(int recurrentID) {
        this.recurrentID = recurrentID;
    }

    public void reactivate() {
        this.wasAlreadyActivated = false;
    }
}
