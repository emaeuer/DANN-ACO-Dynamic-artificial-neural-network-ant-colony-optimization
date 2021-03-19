package de.emaeuer.configuration.value;

import de.emaeuer.configuration.ConfigurationVariable;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serial;
import java.util.Arrays;
import java.util.Map;

public class ExpressionConfigurationValue extends AbstractConfigurationValue<Double> {

    private final static Logger LOG = LogManager.getLogger(ExpressionConfigurationValue.class);

    @Serial
    private static final long serialVersionUID = -8223344718883992821L;

    private static final Function[] ADDITIONAL_FUNCTIONS = new Function[]{
            new Function("max", 2) {
                @Override
                public double apply(double... doubles) {
                    return Math.max(doubles[0], doubles[1]);
                }
            },
            new Function("min", 2) {
                @Override
                public double apply(double... doubles) {
                    return Math.min(doubles[0], doubles[1]);
                }
            }
    };

    private static final Operator[] ADDITIONAL_OPERATORS = new Operator[]{};

    private String expressionString;

    private transient Expression expression; // transient --> exclude expression from serialization because is not serializable

    private final Class<? extends ConfigurationVariable> variables;

    public ExpressionConfigurationValue(String value, Class<? extends ConfigurationVariable> parameters) {
        super(value);
        this.variables = parameters;
    }

    @Override
    public void setValue(String value) {
        this.expressionString = value;
        this.expression = null; // just invalidate expression because it is initialized lazily
    }

    @Override
    public String getStringRepresentation() {
        return this.expressionString;
    }

    @Override
    public Double getValueForState(Map<String, Double> variables) {
        if (this.expression == null) {
            // lazy initialization of expression because of serialization
            initializeExpression();
        }

        try {
            return this.expression
                    .setVariables(variables)
                    .evaluate();
        } catch (ArithmeticException e) {
            LOG.log(Level.WARN, "Division by zero detected. Returned 0 as result", e);
            return 0.0;
        }
    }

    @Override
    public AbstractConfigurationValue<Double> copy() {
        return new ExpressionConfigurationValue(this.expressionString, this.variables);
    }

    private void initializeExpression() {
        String[] variables = Arrays.stream(this.variables.getEnumConstants())
                .map(ConfigurationVariable::getEquationAbbreviation)
                .toArray(String[]::new);

        this.expression = new ExpressionBuilder(this.expressionString)
                .functions(ADDITIONAL_FUNCTIONS)
                .operator(ADDITIONAL_OPERATORS)
                .variables(variables)
                .build();
    }

    public Class<? extends ConfigurationVariable> getVariables() {
        return this.variables;
    }

}
