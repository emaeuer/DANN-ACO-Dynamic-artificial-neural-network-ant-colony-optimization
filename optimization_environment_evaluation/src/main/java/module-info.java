module OptimizationEnvironmentEvaluation {
    exports de.emaeuer.evaluation;
    exports de.emaeuer.cli;

    requires particle.environment.engine;
    requires execution.state;
    requires optimization;
    requires commons.math3;
    requires org.apache.logging.log4j;
    requires info.picocli;
    requires java.scripting;

    opens de.emaeuer.cli to info.picocli;
}