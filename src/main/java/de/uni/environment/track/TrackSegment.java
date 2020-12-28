package de.uni.environment.track;

import de.uni.math.Vector2D;

public class TrackSegment {

    private Vector2D startPoint;
    private Vector2D endPoint;
    private Vector2D controlPoint1;
    private Vector2D controlPoint2;

    public TrackSegment(Vector2D startPoint, Vector2D endPoint) {
        setStartPoint(startPoint);
        setControlPoint1(startPoint);
        setEndPoint(endPoint);
        setControlPoint2(endPoint);
    }
    public Vector2D getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Vector2D startPoint) {
        this.startPoint = startPoint;
    }

    public Vector2D getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Vector2D endPoint) {
        this.endPoint = endPoint;
    }

    public Vector2D getControlPoint1() {
        return controlPoint1;
    }

    public void setControlPoint1(Vector2D controlPoint1) {
        this.controlPoint1 = controlPoint1;
    }

    public Vector2D getControlPoint2() {
        return controlPoint2;
    }

    public void setControlPoint2(Vector2D controlPoint2) {
        this.controlPoint2 = controlPoint2;
    }
}
