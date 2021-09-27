package de.emaeuer.environment.math;

import java.util.Locale;

public class Vector2D {

    private double x;
    private double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector2D other) {
        other = checkNull(other);

        this.x = other.x;
        this.y = other.y;
    }

    public Vector2D(double radians) {
        this.x = Math.cos(radians);
        this.y = Math.sin(radians);
    }

    public Vector2D() {
        this.x = 0;
        this.y = 0;
    }

    public Vector2D add(Vector2D other) {
        other = checkNull(other);
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public Vector2D add(double value) {
        this.x += value;
        this.y += value;
        return this;
    }

    public static Vector2D add(Vector2D v1, Vector2D v2) {
        Vector2D result = new Vector2D(v1);
        result.add(checkNull(v2));
        return result;
    }

    public Vector2D multiply(Vector2D other) {
        other = checkNull(other);
        this.x *= other.x;
        this.y *= other.y;
        return this;
    }

    public Vector2D multiply(double value) {
        this.x *= value;
        this.y *= value;
        return this;
    }

    public static Vector2D multiply(Vector2D v, double value) {
        Vector2D result = new Vector2D(v);
        result.x *= value;
        result.y *= value;
        return result;
    }

    public static Vector2D multiply(Vector2D v1, Vector2D v2) {
        Vector2D result = new Vector2D(v1);
        result.multiply(checkNull(v2));
        return result;
    }

    public Vector2D subtract(Vector2D other) {
        other = checkNull(other);
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    public Vector2D subtract(double value) {
        this.x -= value;
        this.y -= value;
        return this;
    }

    public static Vector2D subtract(Vector2D v1, Vector2D v2) {
        Vector2D result = new Vector2D(v1);
        result.subtract(checkNull(v2));
        return result;
    }

    public Vector2D divide(Vector2D other) {
        other = checkNull(other);
        this.x /= other.x;
        this.y /= other.y;
        return this;
    }

    public Vector2D divide(double value) {
        this.x /= value;
        this.y /= value;
        return this;
    }

    public static Vector2D divide(Vector2D v1, Vector2D v2) {
        Vector2D result = new Vector2D(v1);
        result.divide(checkNull(v2));
        return result;
    }

    public double magnitude() {
        return Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2));
    }

    public static double distance(Vector2D a, Vector2D b) {
        double distance = Math.pow(a.x - b.x, 2);
        distance += Math.pow(a.y - b.y, 2);

        return Math.sqrt(distance);
    }

    public Vector2D normalize() {
        double magnitude = magnitude();
        if (magnitude > 0) {
            this.x /= magnitude;
            this.y /= magnitude;
        }
        return this;
    }

    public Vector2D limit(double limit) {
        if (magnitude() > limit) {
            normalize();
            multiply(limit);
        }
        return this;
    }

    public static Vector2D limit(Vector2D v, double limit) {
        Vector2D result = new Vector2D(v);
        if (result.magnitude() > limit) {
            result.normalize();
            result.multiply(limit);
        }
        return result;
    }

    public double angle() {
        return Math.atan2(this.y, this.x);
    }

    public double angle(Vector2D other) {
        other = checkNull(other);
        return Math.acos(dotProduct(other) / (magnitude() * other.magnitude()));
    }

    public double dotProduct(Vector2D other) {
        return this.x * other.x + this.y * other.y;
    }

    private static Vector2D checkNull(Vector2D vector) {
        return vector == null ? new Vector2D() : vector;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%f, %f]", this.x, this.y);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vector2D vector2D = (Vector2D) o;

        if (Double.compare(vector2D.x, x) != 0) return false;
        return Double.compare(vector2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
