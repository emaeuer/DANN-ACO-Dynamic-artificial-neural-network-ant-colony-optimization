package de.emaeuer.environment.impl;

import de.emaeuer.environment.AbstractEnvironment;
import de.emaeuer.environment.elements.Particle;
import de.emaeuer.environment.track.Track;
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
