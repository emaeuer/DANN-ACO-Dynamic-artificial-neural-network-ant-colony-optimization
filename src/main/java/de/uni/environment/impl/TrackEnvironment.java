package de.uni.environment.impl;

import de.uni.environment.AbstractEnvironment;
import de.uni.environment.elements.Particle;
import de.uni.environment.track.Track;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.function.BiConsumer;

public class TrackEnvironment extends AbstractEnvironment {

    private ObjectProperty<Track> track;

    public TrackEnvironment(int particleNumber) {
        super(particleNumber);
    }

    public TrackEnvironment(int particleNumber, BiConsumer<Particle, AbstractEnvironment> borderStrategy) {
        super(particleNumber, borderStrategy);
    }

    @Override
    protected void initialize() {
        this.track = new SimpleObjectProperty<>();
    }

    @Override
    protected void initializeParticles() {

    }

    public Track getTrack() {
        return track.get();
    }

    public ObjectProperty<Track> trackProperty() {
        return track;
    }
}
