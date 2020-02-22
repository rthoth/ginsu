package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import org.locationtech.jts.geom.*;
import org.pcollections.PVector;

import java.util.Objects;

public class Slicer {

    private final PVector<X> x;
    private final PVector<Y> y;

    @SuppressWarnings("unused")
    public Slicer(double[] x, double[] y) {
        this(x, y, Ginsu.DEFAULT_OFFSET, Ginsu.DEFAULT_EXTRUSION);
    }

    public Slicer(double[] x, double[] y, double offset, double extrusion) {
        this(Ginsu.mapTo(x, v -> new X(v, offset, extrusion)), Ginsu.mapTo(y, v -> new Y(v, offset, extrusion)));
    }

    public Slicer(PVector<X> x, PVector<Y> y) {
        isValid(x);
        isValid(y);
        this.x = x;
        this.y = y;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static <K extends Knife<K>> void isValid(PVector<K> vector) {
        if (!vector.isEmpty()) {
            final var iterator = vector.iterator();
            iterator.hasNext();
            var current = iterator.next();

            while (iterator.hasNext()) {
                var next = iterator.next();
                if (current.compareTo(next) < 0)
                    current = next;
                else
                    throw new GinsuException.InvalidSequence(vector.toString());
            }
        }
    }

    public Grid<? extends Geometry> slice(Geometry geometry) {
        return slice(geometry, Order.AUTOMATIC);
    }

    public Grid<? extends Geometry> slice(Geometry geometry, Order order) {
        if (geometry instanceof Polygonal) {
            return polygonal((Polygonal) geometry, order);
        } else if (geometry instanceof Lineal) {
            return lineal((Lineal) geometry, order);
        } else if (geometry instanceof Puntal) {
            return puntual((Puntal) geometry, order);
        } else {
            throw new GinsuException.Unsupported(Objects.toString(geometry, "null"));
        }
    }

    public Grid<MultiPoint> puntual(Puntal puntal) {
        return this.puntual(puntal, Order.AUTOMATIC);
    }

    public Grid<MultiPoint> puntual(Puntal puntal, Order order) {
        throw new GinsuException.Unsupported("");
    }

    public Grid<MultiLineString> lineal(Lineal lineal) {
        return this.lineal(lineal, Order.AUTOMATIC);
    }

    public Grid<MultiLineString> lineal(Lineal lineal, Order order) {
        throw new GinsuException.Unsupported("");
    }

    public Grid<MultiPolygon> polygonal(Polygonal polygonal) {
        return this.polygonal(polygonal, Order.AUTOMATIC);
    }

    public Grid<MultiPolygon> polygonal(Polygonal polygonal, Order order) {
        return new SliceGrid<>(x, y, MultiShape.of(polygonal), new PolygonSlicer(((Geometry) polygonal).getFactory()), order).getResult();
    }
}
