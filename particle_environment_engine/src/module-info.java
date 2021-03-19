module particle.environment.engine {
    requires optimization;
    requires commons.math3;
    requires execution.state;

    exports de.emaeuer.environment;
    exports de.emaeuer.environment.elements;
    exports de.emaeuer.environment.configuration;
    exports de.emaeuer.environment.bird;
    exports de.emaeuer.environment.bird.elements;
    exports de.emaeuer.environment.cartpole;
    exports de.emaeuer.environment.factory;
    exports de.emaeuer.environment.elements.shape;
}