package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.*;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Optional;

public abstract class Shape {

    private final Geometry source;

    public Shape(Geometry source) {
        this.source = source;
    }

    public static Shape of(Polygon polygon) {
        if (!polygon.isEmpty()) {
            var sequences = Ginsu.collect(Ginsu.iterable(polygon),
                                          ring -> !ring.isEmpty() ? Optional.of(ring.getCoordinateSequence()) : Optional.empty());
            return !sequences.isEmpty() ? new NonEmpty(polygon, sequences) : new Empty(polygon);
        } else {
            return new Empty(polygon);
        }
    }

    public static Shape of(MultiLineString multiLineString) {
        if (!multiLineString.isEmpty()) {
            var sequences = Ginsu.collect(Ginsu.<LineString>iterable(multiLineString),
                                          line -> !line.isEmpty() ? Optional.of(line.getCoordinateSequence()) : Optional.empty());
            return !sequences.isEmpty() ? new NonEmpty(multiLineString, sequences) : new Empty(multiLineString);
        } else {
            return new Empty(multiLineString);
        }
    }

    public abstract boolean nonEmpty();

    public abstract PVector<CoordinateSequence> sequences();

    private static class Empty extends Shape {

        public Empty(Geometry source) {
            super(source);
        }

        @Override
        public boolean nonEmpty() {
            return false;
        }

        @Override
        public PVector<CoordinateSequence> sequences() {
            return TreePVector.empty();
        }
    }

    private static class NonEmpty extends Shape {

        private final PVector<CoordinateSequence> sequences;

        public NonEmpty(Geometry source, PVector<CoordinateSequence> sequences) {
            super(source);
            this.sequences = sequences;
        }

        @Override
        public boolean nonEmpty() {
            return true;
        }

        @Override
        public PVector<CoordinateSequence> sequences() {
            return sequences;
        }
    }
}
