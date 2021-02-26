package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;

public abstract class Knife {

    public final double value;
    public final double offset;
    public final double extrusion;
    public final Dimension dimension;

    public Knife(double value, double offset, double extrusion, Dimension dimension) {
        this.value = value;
        this.offset = Math.abs(offset);
        this.extrusion = extrusion;
        this.dimension = dimension;
    }

    public abstract <K extends Knife> K getLower();

    public abstract <K extends Knife> K getUpper();

    public abstract Coordinate intersection(Coordinate a, Coordinate b);

    public abstract double ordinate(Coordinate coordinate);

    public abstract int position(Coordinate coordinate);

    public static class X extends Knife {

        private final X upper;
        private final X lower;

        public X(double value, double offset, double extrusion) {
            super(value, offset, extrusion, Dimension.X);
            if (extrusion != 0D) {
                upper = new X(value + extrusion, offset, 0D);
                lower = new X(value - extrusion, offset, 0D);
            } else {
                lower = upper = this;
            }
        }

        public X extrude(double extrusion) {
            return this.extrusion != extrusion ? new X(value, offset, extrusion) : this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K extends Knife> K getLower() {
            return (K) lower;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K extends Knife> K getUpper() {
            return (K) upper;
        }

        @Override
        public Coordinate intersection(Coordinate a, Coordinate b) {
            var y = a.getY() + ((b.getY() - a.getY()) * (value - a.getX())) / (b.getX() - a.getX());
            return new Coordinate(value, y);
        }

        @Override
        public double ordinate(Coordinate coordinate) {
            return coordinate.getY();
        }

        @Override
        public int position(Coordinate coordinate) {
            return Ginsu.compare(coordinate.getX(), value, offset);
        }

        @Override
        public String toString() {
            return "X(" + value + ")";
        }
    }

    public static class Y extends Knife {

        private final Y upper;
        private final Y lower;

        public Y(double value, double offset, double extrusion) {
            super(value, offset, extrusion, Dimension.Y);
            if (extrusion != 0D) {
                upper = new Y(value + extrusion, offset, 0D);
                lower = new Y(value - extrusion, offset, 0D);
            } else {
                lower = upper = this;
            }
        }

        public Y extrude(double extrusion) {
            return this.extrusion != extrusion ? new Y(value, offset, extrusion) : this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K extends Knife> K getLower() {
            return (K) lower;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K extends Knife> K getUpper() {
            return (K) upper;
        }

        @Override
        public Coordinate intersection(Coordinate a, Coordinate b) {
            var x = a.getX() + ((b.getX() - a.getX()) * (value - a.getY())) / (b.getY() - a.getY());
            return new Coordinate(x, value);
        }

        @Override
        public double ordinate(Coordinate coordinate) {
            return coordinate.getX();
        }

        @Override
        public int position(Coordinate coordinate) {
            return Ginsu.compare(coordinate.getY(), value, offset);
        }

        @Override
        public String toString() {
            return "Y(" + value + ")";
        }
    }
}
