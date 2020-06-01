package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.*;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

public abstract class MultiShape implements Iterable<Shape>, Mergeable<MultiShape> {

    public static final MultiShape EMPTY = new Empty();

    public static MultiShape of(Polygonal polygonal) {
        PVector<Shape> shapes = TreePVector.empty();

        if (polygonal instanceof MultiPolygon) {
            var multiPolygon = (MultiPolygon) polygonal;
            for (var i = 0; i < multiPolygon.getNumGeometries(); i++) {
                var polygon = (Polygon) multiPolygon.getGeometryN(i);
                var shape = Shape.of(polygon);
                if (shape.nonEmpty())
                    shapes = shapes.plus(shape);
            }
        } else if (polygonal instanceof Polygon) {
            var shape = Shape.of((Polygon) polygonal);
            if (shape.nonEmpty())
                shapes = shapes.plus(shape);
        } else {
            throw new GinsuException.IllegalArgument(Objects.toString(polygonal));
        }

        return !shapes.isEmpty() ? new NotEmpty(shapes, (Geometry) polygonal) : EMPTY;
    }

    public static MultiShape of(Shape shape) {
        return shape.nonEmpty() ? new NotEmpty(TreePVector.singleton(shape), null) : EMPTY;
    }

    public static MultiShape of(Iterable<Shape> shapes) {
        var vector = TreePVector.<Shape>empty();
        for (var shape : shapes) {
            if (shape.nonEmpty())
                vector = vector.plus(shape);
        }

        return !vector.isEmpty() ? new NotEmpty(vector, null) : EMPTY;
    }

    public abstract Geometry getSource();

    public abstract boolean nonEmpty();

    public abstract MultiPolygon toMultiPolygon(GeometryFactory factory);

    private static class Empty extends MultiShape {

        @Override
        public Geometry getSource() {
            return null;
        }

        @Override
        public Iterator<Shape> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean nonEmpty() {
            return false;
        }

        @Override
        public MultiShape plus(MultiShape other) {
            return other;
        }

        @Override
        public MultiPolygon toMultiPolygon(GeometryFactory factory) {
            return factory.createMultiPolygon();
        }
    }

    private static class NotEmpty extends MultiShape {

        private final PVector<Shape> shapes;
        private final Geometry source;

        public NotEmpty(PVector<Shape> shapes, Geometry source) {
            this.shapes = shapes;
            this.source = source;
        }

        @Override
        public Geometry getSource() {
            return source;
        }

        @Override
        public Iterator<Shape> iterator() {
            return shapes.iterator();
        }

        @Override
        public boolean nonEmpty() {
            return true;
        }

        @Override
        public MultiShape plus(MultiShape other) {
            return other instanceof NotEmpty ? new NotEmpty(shapes.plusAll(((NotEmpty) other).shapes), null) : this;
        }

        @Override
        public MultiPolygon toMultiPolygon(GeometryFactory factory) {
            if (!(source instanceof MultiPolygon))
                return factory.createMultiPolygon(Ginsu.map(shapes, shape -> shape.toPolygon(factory)).toArray(Polygon[]::new));
            else
                return (MultiPolygon) source;
        }
    }
}
