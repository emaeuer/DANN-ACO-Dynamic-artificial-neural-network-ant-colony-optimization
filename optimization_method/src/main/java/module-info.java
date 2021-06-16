module optimization {
    exports de.emaeuer.optimization;
    exports de.emaeuer.optimization.configuration;
    exports de.emaeuer.optimization.factory;
    exports de.emaeuer.optimization.paco.configuration;
    exports de.emaeuer.optimization.util;

    requires commons.math3;
    requires exp4j;
    requires execution.state;
    requires neural.network;
    requires org.apache.logging.log4j;
    requires com.google.common;
    requires neat.anji;
}