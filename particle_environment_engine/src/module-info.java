module particle.environment.engine {
    requires optimization;
    requires ant.colony.optimization;
    requires javafx.base;
    requires javafx.controls;
    requires commons.math3;

    exports de.emaeuer.environment.impl;
    exports de.emaeuer.environment;
    exports de.emaeuer.environment.elements;
}