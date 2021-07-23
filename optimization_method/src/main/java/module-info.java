module optimization {
    exports de.emaeuer.optimization;
    exports de.emaeuer.optimization.configuration;
    exports de.emaeuer.optimization.factory;
    exports de.emaeuer.optimization.paco.configuration;
    exports de.emaeuer.optimization.util;
    exports de.emaeuer.optimization.neat.configuration;
    exports de.emaeuer.optimization.paco.state;

    requires commons.math3;
    requires exp4j;
    requires execution.state;
    requires neural.network;
    requires org.apache.logging.log4j;
    requires com.google.common;
    requires neat.anji;
}