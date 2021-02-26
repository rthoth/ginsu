package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Polygonal;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public abstract class MultiShape {

    private final Geometry source;

    public MultiShape(Geometry source) {
        this.source = source;
    }

    public static MultiShape of(Polygonal polygonal) {
        if (polygonal instanceof Polygon) {
            var shape = Shape.of((Polygon) polygonal);
            return shape.nonEmpty() ? new NonEmpty((Geometry) polygonal, TreePVector.singleton(shape)) : new Empty((Geometry) polygonal);
        } else if (polygonal instanceof MultiPolygon) {
            var shapes = Ginsu.collect(Ginsu.<Polygon>iterable((MultiPolygon) polygonal),
                                       polygon -> {
                                           var shape = Shape.of(polygon);
                                           return shape.nonEmpty() ? Optional.of(shape) : Optional.empty();
                                       });

            return !shapes.isEmpty() ? new NonEmpty((Geometry) polygonal, shapes) : new Empty((Geometry) polygonal);
        } else {
            throw new GinsuException.InvalidArgument(Objects.toString(polygonal));
        }
    }

    public abstract boolean nonEmpty();

    public abstract Iterable<Shape> shapes();

    private static class Empty extends MultiShape {

        public Empty(Geometry source) {
            super(source);
        }

        @Override
        public boolean nonEmpty() {
            return false;
        }

        @Override
        public Iterable<Shape> shapes() {
            return Collections.emptyList();
        }
    }

    private static class NonEmpty extends MultiShape {

        private final PVector<Shape> shapes;

        public NonEmpty(Geometry source, PVector<Shape> shapes) {
            super(source);
            this.shapes = shapes;
        }

        @Override
        public boolean nonEmpty() {
            return true;
        }

        @Override
        public Iterable<Shape> shapes() {
            return shapes;
        }
    }
}
