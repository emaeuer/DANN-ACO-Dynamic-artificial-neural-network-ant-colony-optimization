module particle.environment.gui {
    requires javafx.graphics;
    requires particle.environment.engine;
    requires javafx.fxml;
    requires javafx.controls;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;
    requires optimization;
    requires execution.state;
    requires JavaFXSmartGraph;
    requires java.logging;
    requires optimization.environment.evaluation;

    exports de.emaeuer to javafx.graphics;
    exports de.emaeuer.logging to org.apache.logging.log4j.core;

    opens de.emaeuer.gui.controller to javafx.fxml;
}