import de.emaeuer.aco.AcoHandler;
import de.emaeuer.aco.Ant;

module ant.colony.optimization {
    exports de.emaeuer.aco;
    exports de.emaeuer.aco.configuration;
    requires commons.math3;
    requires neural.network;
    requires optimization;
}