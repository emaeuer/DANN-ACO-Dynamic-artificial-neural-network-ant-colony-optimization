package de.emaeuer.ann.impl.neuron.based;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.NeuronID;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NeuronBasedNeuralNetworkBuilder {

    private final ConfigurationHandler<NeuralNetworkConfiguration> configuration;

    private Neuron biasNeuron = null;
    private List<Neuron> inputLayer = new ArrayList<>();
    private List<Neuron> hiddenLayer = new ArrayList<>();
    private List<Neuron> outputLayer = new ArrayList<>();

    private boolean fullyConnectToNextLayer = false;

    private NeuronBasedNeuralNetworkBuilder(ConfigurationHandler<NeuralNetworkConfiguration> configuration) {
        this.configuration = configuration;
    }

    public static NeuronBasedNeuralNetworkBuilder buildWithConfiguration(ConfigurationHandler<NeuralNetworkConfiguration> configuration) {
        configuration.logConfiguration();
        return new NeuronBasedNeuralNetworkBuilder(configuration);
    }

    public static NeuronBasedNeuralNetworkBuilder build() {
        return buildWithConfiguration(new ConfigurationHandler<>(NeuralNetworkConfiguration.class));
    }

    public static NeuronBasedNeuralNetwork buildFromNeuronCollection(List<Neuron> neurons) {
        NeuronBasedNeuralNetworkBuilder builder = build();
        for (Neuron neuron : neurons) {
            switch (neuron.getType()) {
                case INPUT -> builder.inputLayer.add(neuron);
                case BIAS -> builder.biasNeuron = neuron;
                case HIDDEN -> builder.hiddenLayer.add(neuron);
                case OUTPUT -> builder.outputLayer.add(neuron);
            }
        }
        return builder.finish();
    }

    public NeuronBasedNeuralNetworkBuilder fullyConnectToNextLayer() {
        this.fullyConnectToNextLayer = true;
        return this;
    }

    public NeuronBasedNeuralNetworkBuilder implicitBias() {
        this.biasNeuron = Neuron.build()
                .type(NeuronType.BIAS)
                .activationFunction(ActivationFunction.IDENTITY)
                .id(new NeuronID(0, 0))
                .finish();
        return this;
    }

    public NeuronBasedNeuralNetworkBuilder inputLayer() {
        int size = this.configuration.getValue(NeuralNetworkConfiguration.INPUT_LAYER_SIZE, Integer.class);
        ActivationFunction activationFunction = ActivationFunction.valueOf(this.configuration.getValue(NeuralNetworkConfiguration.INPUT_ACTIVATION_FUNCTION, String.class));

        int start = this.biasNeuron == null ? 0 : 1;
        this.inputLayer = IntStream.range(start, size + start)
                .mapToObj(i -> new NeuronID(0, i))
                .map(id -> Neuron.build().id(id))
                .map(b -> b.type(NeuronType.INPUT))
                .map(b -> b.activationFunction(activationFunction))
                .map(b -> b.bias(0))
                .map(Neuron.NeuronBuilder::finish)
                .collect(Collectors.toList());

        return this;
    }

    public NeuronBasedNeuralNetworkBuilder outputLayer() {
        int size = this.configuration.getValue(NeuralNetworkConfiguration.OUTPUT_LAYER_SIZE, Integer.class);
        ActivationFunction activationFunction = ActivationFunction.valueOf(this.configuration.getValue(NeuralNetworkConfiguration.OUTPUT_ACTIVATION_FUNCTION, String.class));

        this.outputLayer = IntStream.range(0, size)
                .mapToObj(i -> new NeuronID(2, i))
                .map(id -> Neuron.build().id(id))
                .map(b -> b.type(NeuronType.OUTPUT))
                .map(b -> b.activationFunction(activationFunction))
                .map(b -> b.bias(0))
                .map(Neuron.NeuronBuilder::finish)
                .collect(Collectors.toList());

        fullyConnectToNextLayerIfNecessary(false);

        return this;
    }

    public NeuronBasedNeuralNetworkBuilder hiddenLayer(int size) {
        ActivationFunction activationFunction = ActivationFunction.valueOf(this.configuration.getValue(NeuralNetworkConfiguration.HIDDEN_ACTIVATION_FUNCTION, String.class));

        this.hiddenLayer = IntStream.range(0, size)
                .mapToObj(i -> new NeuronID(1, i))
                .map(id -> Neuron.build().id(id))
                .map(b -> b.type(NeuronType.HIDDEN))
                .map(b -> b.activationFunction(activationFunction))
                .map(b -> b.bias(0))
                .map(Neuron.NeuronBuilder::finish)
                .collect(Collectors.toList());

        fullyConnectToNextLayerIfNecessary(true);

        return this;
    }

    private void fullyConnectToNextLayerIfNecessary(boolean isHidden) {
        if (!this.fullyConnectToNextLayer) {
            return;
        }

        List<Neuron> prevLayer = isHidden
                ? this.inputLayer
                : this.hiddenLayer.isEmpty() ? this.inputLayer : this.hiddenLayer;
        List<Neuron> currentLayer = isHidden
                ? this.hiddenLayer
                : this.outputLayer;

        if (this.biasNeuron != null && prevLayer == this.inputLayer) {
            // copy and add bias in case of implicit bias
            prevLayer = new ArrayList<>(prevLayer);
            prevLayer.add(this.biasNeuron);
        }

        for (Neuron source : prevLayer) {
            for (Neuron target : currentLayer) {
                target.modify().addInput(source, 0);
            }
        }

        this.fullyConnectToNextLayer = false;
    }

    public NeuronBasedNeuralNetwork finish() {
        return new NeuronBasedNeuralNetwork(this.configuration, this.biasNeuron, this.inputLayer, this.hiddenLayer, this.outputLayer);
    }

}
