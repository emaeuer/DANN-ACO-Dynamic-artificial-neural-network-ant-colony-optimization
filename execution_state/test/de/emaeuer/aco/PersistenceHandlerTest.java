package de.emaeuer.aco;

import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationVariable;
import de.emaeuer.configuration.DefaultConfiguration;
import de.emaeuer.configuration.value.AbstractConfigurationValue;
import de.emaeuer.configuration.value.ExpressionConfigurationValue;
import de.emaeuer.configuration.value.IntegerConfigurationValue;
import de.emaeuer.configuration.value.StringConfigurationValue;
import de.emaeuer.persistence.PersistenceHandler;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersistenceHandlerTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private enum TestParameter implements ConfigurationVariable {
        A("Variable A", "a"),
        B("Variable B", "b"),
        C("Variable C", "c");

        private final String name;
        private final String abbreviation;


        TestParameter(String name, String abbreviation) {
            this.name = name;
            this.abbreviation = abbreviation;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getEquationAbbreviation() {
            return this.abbreviation;
        }
    }

    private enum TestEnum implements DefaultConfiguration<TestEnum> {
        TEST_VALUE_1("v1", new ExpressionConfigurationValue("a+b", TestParameter.class)),
        TEST_VALUE_2("v2", new StringConfigurationValue("Test", "Test", "TEST", "test")),
        TEST_VALUE_3("v3", new IntegerConfigurationValue(5));

        private final String name;
        private final AbstractConfigurationValue<?> defaultValue;

        TestEnum(String name, AbstractConfigurationValue<?> defaultValue) {
            this.defaultValue = defaultValue;
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public AbstractConfigurationValue<?> getDefaultValue() {
            return defaultValue;
        }

        @Override
        public Class<?> getValueType() {
            return this.defaultValue.getClass();
        }

        @Override
        public void executeChangeAction(AbstractConfigurationValue<?> newValue, ConfigurationHandler<TestEnum> handler) {
            // do nothing not needed
        }

        @Override
        public boolean refreshNecessary() {
            return false;
        }

        @Override
        public String getKeyName() {
            return name();
        }
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testPersistConfiguration() {
        ConfigurationHandler<TestEnum> source = new ConfigurationHandler<>(TestEnum.class);
        source.setValue(TestEnum.TEST_VALUE_1, new ExpressionConfigurationValue("ab", TestParameter.class));
        source.setValue(TestEnum.TEST_VALUE_2, new StringConfigurationValue("TEST", "Test", "TEST", "test"));
        source.setValue(TestEnum.TEST_VALUE_3, new IntegerConfigurationValue(2));
        source.setName("TestConfig");

        PersistenceHandler.persistObject(source);

        ConfigurationHandler<TestEnum> persisted = new ConfigurationHandler<>(TestEnum.class);
        persisted.setName("TestConfig");
        PersistenceHandler.loadObject(persisted);

        assertEquals(source.getStringRepresentation(TestEnum.TEST_VALUE_1),
                persisted.getStringRepresentation(TestEnum.TEST_VALUE_1));
        assertEquals(source.getValue(TestEnum.TEST_VALUE_2, String.class),
                persisted.getValue(TestEnum.TEST_VALUE_2, String.class));
        assertEquals(source.getValue(TestEnum.TEST_VALUE_3, Integer.class),
                persisted.getValue(TestEnum.TEST_VALUE_3, Integer.class));
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
