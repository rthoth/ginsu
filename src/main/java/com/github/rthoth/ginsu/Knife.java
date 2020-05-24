package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;

public abstract class Knife<K extends Knife<?>> implements Comparable<K> {

    protected final double offset;
    protected final double value;

    public Knife(double value, double offset) {
        this.value = value;
        this.offset = offset;
    }

    public abstract K getLower();

    public abstract K getUpper();

    public abstract Coordinate intersection(Coordinate origin, Coordinate target);

    public abstract int positionOf(Coordinate coordinate);

    public abstract double ordinateOf(Coordinate coordinate);

    public abstract K extrude(double extrusion);

    public static class X extends Knife<X> {

        private final X upper;
        private final X lower;

        public X(double value, double offset, double extrusion) {
            super(value, offset);
            if (extrusion != 0D) {
                lower = new X(value - extrusion, offset, 0D);
                upper = new X(value + extrusion, offset, 0D);
            } else {
                upper = this;
                lower = this;
            }
        }

        @Override
        public double ordinateOf(Coordinate coordinate) {
            return coordinate.getY();
        }

        @Override
        public int compareTo(X other) {
            return Ginsu.compare(value, offset, other.value);
        }

        @Override
        public X extrude(double extrusion) {
            return new X(value, offset, extrusion);
        }

        @Override
        public X getLower() {
            return lower;
        }

        @Override
        public X getUpper() {
            return upper;
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target) {
            final var xo = origin.getX();
            final var yo = origin.getY();
            final var xt = target.getX();
            final var yt = target.getY();

            return new Coordinate(value, ((yt - yo) * (value - xo)) / (xt - xo) + yo);
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            return Ginsu.compare(coordinate.getX(), offset, value);
        }

        @Override
        public String toString() {
            return "X(" + value + ")";
        }
    }

    public static class Y extends Knife<Y> {
        private final Y lower;
        private final Y upper;

        public Y(double value, double offset, double extrusion) {
            super(value, offset);
            if (extrusion != 0D) {
                upper = new Y(value + extrusion, offset, 0D);
                lower = new Y(value - extrusion, offset, 0D);
            } else {
                upper = this;
                lower = this;
            }
        }

        @Override
        public Y extrude(double extrusion) {
            return new Y(value, offset, extrusion);
        }

        @Override
        public double ordinateOf(Coordinate coordinate) {
            return coordinate.getX();
        }

        @Override
        public int compareTo(Y other) {
            return Ginsu.compare(value, offset, other.value);
        }

        @Override
        public Y getLower() {
            return lower;
        }

        @Override
        public Y getUpper() {
            return upper;
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target) {
            final var xo = origin.getX();
            final var yo = origin.getY();
            final var yt = target.getY();
            final var xt = target.getX();

            return new Coordinate(((xt - xo) * (value - yo)) / (yt - yo) + xo, value);
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            return Ginsu.compare(coordinate.getY(), offset, value);
        }

        @Override
        public String toString() {
            return "Y(" + value + ")";
        }
    }
}
