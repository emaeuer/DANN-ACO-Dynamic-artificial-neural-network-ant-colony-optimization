module particle.environment.gui {
    requires javafx.graphics;
    requires particle.environment.engine;
    requires javafx.fxml;
    requires javafx.controls;

    exports de.emaeuer to javafx.graphics;

    opens de.emaeuer.gui.controller to javafx.fxml;
}