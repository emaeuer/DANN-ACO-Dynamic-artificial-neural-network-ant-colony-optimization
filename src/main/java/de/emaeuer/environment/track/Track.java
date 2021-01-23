package de.emaeuer.environment.track;

import de.emaeuer.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class Track {

    private boolean started = false;
    private boolean closed = false;

    private Vector2D startPoint = null;

    private final List<TrackSegment> segments = new ArrayList<>();
    private TrackSegment tempSegment = null;

    public void start(Vector2D startPoint) {
        this.started = true;
        this.startPoint = startPoint;
        this.tempSegment = new TrackSegment(startPoint, startPoint);
    }

    public void persistTemporaryTrackSegment() {
        this.closed = this.tempSegment.getEndPoint().equals(this.startPoint);
        this.segments.add(this.tempSegment);
        this.tempSegment = this.closed ? null : new TrackSegment(this.tempSegment.getEndPoint(), this.tempSegment.getEndPoint());
    }

    public void changeTemporaryTrackSegmentEndPoint(Vector2D endPoint) {
        this.tempSegment.setEndPoint(endPoint);
    }

    public Vector2D getStartPoint() {
        return startPoint;
    }

    public List<TrackSegment> getTrackSegments() {
        return this.segments;
    }

    public TrackSegment getTemporaryTrackSegment() {
        return this.tempSegment;
    }

    public boolean hasBeenStarted() {
        return started;
    }

    public boolean hasBeenClosed() {
        return closed;
    }
}
