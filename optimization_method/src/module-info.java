module optimization {
    exports de.emaeuer.optimization;
    exports de.emaeuer.optimization.configuration;
    exports de.emaeuer.optimization.aco.configuration;
    exports de.emaeuer.optimization.aco;
    exports de.emaeuer.optimization.factory;

    requires commons.math3;
    requires exp4j;
    requires execution.state;
    requires neural.network;
    requires org.apache.logging.log4j;
    requires org.jgrapht.core;
    requires com.google.common;
}