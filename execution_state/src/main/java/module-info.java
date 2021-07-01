module execution.state {
    exports de.emaeuer.state;
    exports de.emaeuer.configuration;
    exports de.emaeuer.configuration.value;
    exports de.emaeuer.state.value;
    exports de.emaeuer.persistence;
    exports de.emaeuer.state.value.data;

    requires exp4j;
    requires org.apache.logging.log4j;
    requires org.json;
}