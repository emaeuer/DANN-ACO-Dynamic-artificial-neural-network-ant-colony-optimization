module particle.environment.engine {
    requires commons.math3;
    requires com.google.common;
    requires execution.state;
    requires optimization;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    exports de.emaeuer.environment;
    exports de.emaeuer.environment.elements;
    exports de.emaeuer.environment.configuration;
    exports de.emaeuer.environment.bird;
    exports de.emaeuer.environment.bird.elements;
    exports de.emaeuer.environment.balance.onedim.configuration;
    exports de.emaeuer.environment.factory;
    exports de.emaeuer.environment.elements.shape;
    exports de.emaeuer.environment.xor;
    exports de.emaeuer.environment.bird.configuration;
    exports de.emaeuer.environment.balance.onedim;
    exports de.emaeuer.environment.balance.twodim;
}