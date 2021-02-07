package de.emaeuer.environment;

import de.emaeuer.environment.elements.Particle;
import de.emaeuer.environment.util.EnvironmentHelper;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart.Series;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractEnvironment {

    private final IntegerProperty particleNumber = new SimpleIntegerProperty();

    private final DoubleProperty width = new SimpleDoubleProperty(800);
    private final DoubleProperty height = new SimpleDoubleProperty(800);

    private final List<Particle> particles = new ArrayList<>();

    private final ListProperty<Series<Integer, Double>> fitnessData = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BiConsumer<Particle, AbstractEnvironment> borderStrategy;

    public AbstractEnvironment(int particleNumber) {
        this(particleNumber, EnvironmentHelper.GO_TO_OTHER_SIDE);
    }

    public AbstractEnvironment(int particleNumber, BiConsumer<Particle, AbstractEnvironment> borderStrategy) {
        this.borderStrategy = borderStrategy;
        setParticleNumber(particleNumber);

        initialize();

        initializeParticles();
    }

    protected abstract void initialize();

    protected abstract void initializeParticles();

    protected abstract void updateFitness();

    protected abstract void restart();

    public void update() {
        this.particles.stream()
                .peek(Particle::step)
                .forEach(p -> this.borderStrategy.accept(p, this));
    }

    public double getWidth() {
        return width.get();
    }

    public DoubleProperty widthProperty() {
        return width;
    }

    public void setWidth(double width) {
        this.width.set(width);
    }

    public double getHeight() {
        return height.get();
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    public void setHeight(double height) {
        this.height.set(height);
    }

    public int getParticleNumber() {
        return particleNumber.get();
    }

    public IntegerProperty particleNumberProperty() {
        return particleNumber;
    }

    public void setParticleNumber(int particleNumber) {
        this.particleNumber.set(particleNumber);
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public ListProperty<Series<Integer, Double>> getFitnessData() {
        return fitnessData;
    }

    public ListProperty<Series<Integer, Double>> fitnessDataProperty() {
        return fitnessData;
    }

    public void setFitnessData(ObservableList<Series<Integer, Double>> fitnessData) {
        this.fitnessData.set(fitnessData);
    }

}
