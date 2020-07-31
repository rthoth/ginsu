package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import org.locationtech.jts.geom.*;
import org.pcollections.PVector;

import java.util.Objects;

public class Slicer {

    private final PVector<X> x;
    private final PVector<Y> y;
    private final double offset;
    private final double extrusion;

    @SuppressWarnings("unused")
    public Slicer(double[] x, double[] y) {
        this(x, y, Ginsu.DEFAULT_OFFSET, Ginsu.DEFAULT_EXTRUSION);
    }

    public Slicer(double[] x, double[] y, double offset, double extrusion) {
        this(Ginsu.map(x, v -> new X(v, offset, extrusion)), Ginsu.map(y, v -> new Y(v, offset, extrusion)), offset, extrusion);
    }

    protected Slicer(PVector<X> x, PVector<Y> y, double offset, double extrusion) {
        isValid(x);
        isValid(y);
        this.x = x;
        this.y = y;
        this.offset = offset;
        this.extrusion = extrusion;
    }

    private static <K extends Knife<K>> void isValid(PVector<K> vector) {
        if (!vector.isEmpty()) {
            final var iterator = vector.iterator();
            var current = Ginsu.next(iterator);

            while (iterator.hasNext()) {
                var next = iterator.next();
                if (current.compareTo(next) < 0)
                    current = next;
                else
                    throw new GinsuException.InvalidSequence(vector.toString());
            }
        }
    }

    public Slicer extrude(double extrusion) {
        if (extrusion != this.extrusion) {
            return new Slicer(Ginsu.map(x, k -> k.extrude(extrusion)), Ginsu.map(y, k -> k.extrude(extrusion)), offset, extrusion);
        } else {
            return this;
        }
    }

    @SuppressWarnings("unused")
    public Grid<MultiLineString> lineal(Lineal lineal) {
        return this.lineal(lineal, Order.AUTOMATIC);
    }

    public Grid<MultiLineString> lineal(Lineal lineal, Order order) {
        throw new GinsuException.Unsupported();
    }

    public Merger merger() {
        return new Merger(x, y, offset);
    }

    @SuppressWarnings("unused")
    public Grid<MultiPolygon> polygonal(Polygonal polygonal) {
        return this.polygonal(polygonal, Order.AUTOMATIC);
    }

    public Grid<MultiPolygon> polygonal(Polygonal polygonal, Order order) {
        return new SliceGrid<>(x, y, new PolygonSlicer(((Geometry) polygonal).getFactory())).apply(MultiShape.of(polygonal), order);
    }

    @SuppressWarnings("unused")
    public Grid<MultiPoint> puntual(Puntal puntal) {
        return this.puntual(puntal, Order.AUTOMATIC);
    }

    public Grid<MultiPoint> puntual(Puntal puntal, Order order) {
        throw new GinsuException.Unsupported();
    }

    @SuppressWarnings("unused")
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
}
