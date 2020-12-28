package de.uni.gui.controller;

import de.uni.environment.impl.TrackEnvironment;
import de.uni.environment.track.Track;
import de.uni.environment.track.TrackSegment;
import de.uni.math.Vector2D;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public class TrackController extends AbstractController<TrackEnvironment> {

    private final ObjectProperty<Track> track = new SimpleObjectProperty<>(new Track());

    @Override
    protected TrackEnvironment getEnvironmentImplementation() {
        return new TrackEnvironment(5);
    }

    @Override
    protected void nextFrame() {
        super.nextFrame();
        if (this.track.isNotNull().get()) {
            drawTrack();
        }
    }

    private void drawTrack() {
        if (!this.track.get().hasBeenStarted()) {
            return;
        }

        GraphicsContext context = getGraphicsContext();

        context.beginPath();
        // draw all persisted segments
        this.track.get().getTrackSegments()
                .forEach(s -> drawTrackSegment(s, context));
        // draw temporary segment
        if (!this.track.get().hasBeenClosed()) {
            drawTrackSegment(this.track.get().getTemporaryTrackSegment(), context);
        }
        context.closePath();
        context.stroke();
    }

    private void drawTrackSegment(TrackSegment segment, GraphicsContext context) {
        Vector2D startPoint = segment.getStartPoint();
        Vector2D endPoint = segment.getEndPoint();
        Vector2D controlPoint1 = segment.getControlPoint1();
        Vector2D controlPoint2 = segment.getControlPoint2();

        context.moveTo(startPoint.getX(), startPoint.getY());
        context.bezierCurveTo(controlPoint1.getX(), controlPoint1.getY(), controlPoint2.getX(), controlPoint2.getY(),
                endPoint.getX(), endPoint.getY());

    }

    @Override
    public void mouseOver(MouseEvent mouseEvent) {
        if (!this.track.get().hasBeenStarted() || this.track.get().hasBeenClosed()) {
            return;
        }

        Vector2D trackStartPoint = this.track.get().getStartPoint();
        Vector2D endPoint = new Vector2D(mouseEvent.getX(), mouseEvent.getY());

        // connect to start point
        if (Vector2D.subtract(trackStartPoint, endPoint).magnitude() < 20) {
            endPoint = trackStartPoint;
        }

        this.track.get().changeTemporaryTrackSegmentEndPoint(endPoint);
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (!this.track.get().hasBeenStarted()) {
            this.track.get().start(new Vector2D(mouseEvent.getX(), mouseEvent.getY()));
        } else if (!this.track.get().hasBeenClosed()) {
            this.track.get().persistTemporaryTrackSegment();
        } else {

        }
    }
}
