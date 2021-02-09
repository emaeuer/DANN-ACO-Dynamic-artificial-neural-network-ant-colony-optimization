package de.emaeuer.optimization.configuration;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.Arrays;


public class ConfigurationValue<T extends ConfigurationKey> implements java.util.function.Function<OptimizationParameter<?>, Double> {

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

    private final T key;
    private Expression function;

    public ConfigurationValue(T key, String functionExpression, OptimizationParameterNames... variables) {
        this.key = key;
        setFunction(functionExpression, variables);
    }

    public void setFunction(String functionExpression, OptimizationParameterNames... variables) {
        String[] parameters = Arrays.stream(variables)
                .distinct()
                .map(OptimizationParameterNames::getName)
                .toArray(String[]::new);

        this.function = new ExpressionBuilder(functionExpression)
                .variables(parameters)
                .functions(ADDITIONAL_FUNCTIONS)
                .build();
    }

    @Override
    public Double apply(OptimizationParameter<?> state) {
       if (state == null) {
           return function.evaluate();
       } else {
           return function
                   .setVariables(state.toParameters())
                   .evaluate();
       }
    }

    public T getKey() {
        return key;
    }
}
