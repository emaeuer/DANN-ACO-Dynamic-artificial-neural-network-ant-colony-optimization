package de.emaeuer.state.value;

public class StateValueFactory {

    private StateValueFactory() {}

    public static AbstractStateValue<?, ?> createValueForClass(Class<? extends AbstractStateValue<?, ?>> className) {
        if (DataSeriesStateValue.class.equals(className)) {
            return new DataSeriesStateValue();
        } else if (EmbeddedState.class.equals(className)) {
            return new EmbeddedState();
        } else if (MapOfStateValue.class.equals(className)) {
            return new MapOfStateValue();
        } else if (NumberStateValue.class.equals(className)) {
            return new NumberStateValue();
        } else if (GraphStateValue.class.equals(className)) {
            return new GraphStateValue();
        } else if (DistributionStateValue.class.equals(className)) {
            return new DistributionStateValue();
        } else if (ScatteredDataStateValue.class.equals(className)) {
            return new ScatteredDataStateValue();
        }

        throw new IllegalArgumentException("Factory doesn't support values of type " + className.getSimpleName());
    }

}
