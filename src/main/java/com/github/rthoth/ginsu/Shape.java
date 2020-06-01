package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.*;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Collections;
import java.util.Iterator;

public abstract class Shape implements Iterable<CoordinateSequence> {

    public static final Shape EMPTY = new Empty();

    public static Shape of(Polygon polygon) {
        if (!polygon.isEmpty()) {
            PVector<CoordinateSequence> sequences = TreePVector
                    .singleton(polygon.getExteriorRing().getCoordinateSequence());

            for (var i = 0; i < polygon.getNumInteriorRing(); i++) {
                var sequence = polygon.getInteriorRingN(i).getCoordinateSequence();
                if (sequence.size() != 0)
                    sequences = sequences.plus(sequence);
            }

            return new NotEmpty(sequences, polygon);
        } else {
            return EMPTY;
        }
    }

    public static Shape of(CoordinateSequence head, Iterable<CoordinateSequence> tail) {
        var sequences = TreePVector.<CoordinateSequence>empty();
        if (head.size() > 0)
            sequences = sequences.plus(head);

        for (var element : tail) {
            if (element.size() > 0) {
                sequences = sequences.plus(element);
            }
        }

        return !sequences.isEmpty() ? new NotEmpty(sequences, null) : EMPTY;
    }

    public abstract Geometry getSource();

    public abstract boolean nonEmpty();

    public abstract Polygon toPolygon(GeometryFactory factory);

    private static class Empty extends Shape {

        @Override
        public Geometry getSource() {
            return null;
        }

        @Override
        public Iterator<CoordinateSequence> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean nonEmpty() {
            return false;
        }

        @Override
        public Polygon toPolygon(GeometryFactory factory) {
            return factory.createPolygon();
        }
    }

    private static class NotEmpty extends Shape {

        private final PVector<CoordinateSequence> sequences;
        private final Geometry source;

        public NotEmpty(PVector<CoordinateSequence> sequences, Geometry source) {
            this.sequences = sequences;
            this.source = source;
        }

        @Override
        public Geometry getSource() {
            return source;
        }

        @Override
        public Iterator<CoordinateSequence> iterator() {
            return sequences.iterator();
        }

        @Override
        public boolean nonEmpty() {
            return true;
        }

        @Override
        public Polygon toPolygon(GeometryFactory factory) {
            if (!(source instanceof Polygon)) {
                var it = sequences.iterator();
                var shell = factory.createLinearRing(Ginsu.next(it));
                var holes = Ginsu.map(it, factory::createLinearRing);
                return factory.createPolygon(shell, holes.toArray(LinearRing[]::new));
            } else {
                return (Polygon) source;
            }
        }
    }
}
