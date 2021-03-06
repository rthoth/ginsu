package com.github.rthoth.ginsu;

import com.google.common.truth.Correspondence;
import org.locationtech.jts.geom.*;
import org.pcollections.TreePVector;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Util {

    default Correspondence<Geometry, Geometry> compareTopology() {
        return Correspondence.from((actual, expected) -> {
            if (actual != null && expected != null) {
                if (actual != expected) {
                    if (!actual.isEmpty() && !expected.isEmpty()) {
                        return actual.equalsTopo(expected);
                    } else {
                        return actual.isEmpty() && expected.isEmpty();
                    }
                } else {
                    return true;
                }
            }

            return actual == null && expected == null;
        }, "Topology!");
    }

    default <T> Correspondence<T, Predicate<T>> compareWithPredicate() {
        return Correspondence.from((actual, expected) -> expected != null ? expected.test(actual) : actual == null, "Predicate!");
    }

    default Envelope envelope(double xmin, double xmax, double ymin, double ymax) {
        return new Envelope(xmin, xmax, ymin, ymax);
    }

    default Envelope envelopeOf(CoordinateSequence sequence) {
        double xmin = Double.POSITIVE_INFINITY, ymin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;

        for (var i = 0; i < sequence.size(); i++) {
            var coordinate = sequence.getCoordinate(i);
            if (coordinate.getX() < xmin)
                xmin = coordinate.getX();
            if (coordinate.getX() > xmax)
                xmax = coordinate.getX();
            if (coordinate.getY() < ymin)
                ymin = coordinate.getY();
            if (coordinate.getY() > ymax)
                ymax = coordinate.getY();
        }

        return new Envelope(xmin, xmax, ymin, ymax);
    }

    default File file(String filename) {
        return new File(filename);
    }

    default <T> Grid<T> gridXY(int w, int h, List<T> list) {
        return new Grid.XY<>(w, h, TreePVector.from(list));
    }

    default <T> Grid<T> gridYX(int w, int h, List<T> list) {
        return new Grid.YX<>(w, h, TreePVector.from(list));
    }

    default <T> Lazy<T> lazy(Supplier<T> supplier) {
        return new Lazy(supplier);
    }

    default <T> List<T> list(T... elements) {
        return Arrays.asList(elements);
    }

    default <K extends Knife<K>> Slice.Lower<K> lower(K upper) {
        return new Slice.Lower<>(upper);
    }

    default <K extends Knife<K>> Slice.Middle<K> middle(K lower, K upper) {
        return new Slice.Middle<>(lower, upper);
    }

    default void println(Object object) {
        System.out.println(object);
    }

    default MultiPolygon toMultiPolygon(Geometry geometry) {
        if (geometry instanceof MultiPolygon)
            return (MultiPolygon) geometry;
        if (geometry instanceof Polygon)
            return geometry.getFactory().createMultiPolygon(new Polygon[]{(Polygon) geometry});

        throw new IllegalArgumentException();
    }

    default String toWKT(Iterable<Knife.X> x, Iterable<Knife.Y> y, CoordinateSequence sequence, GeometryFactory factory) {
        return toWKT(x, y, envelopeOf(sequence), factory);
    }

    default String toWKT(Iterable<Knife.X> x, Iterable<Knife.Y> y, Geometry geometry) {
        return toWKT(x, y, geometry.getEnvelopeInternal(), geometry.getFactory());
    }

    default String toWKT(Iterable<Knife.X> x, Iterable<Knife.Y> y, Envelope envelope, GeometryFactory factory) {
        envelope.expandBy(envelope.getWidth() * 0.1D, envelope.getHeight() * 0.1D);

        var xs = Ginsu.map(x, k -> factory.createLineString(new Coordinate[]{
                new Coordinate(k.value, envelope.getMinY()),
                new Coordinate(k.value, envelope.getMaxY())
        }));

        var ys = Ginsu.map(y, k -> factory.createLineString(new Coordinate[]{
                new Coordinate(envelope.getMinX(), k.value),
                new Coordinate(envelope.getMaxX(), k.value)
        }));

        return factory.createMultiLineString(xs.plusAll(ys).toArray(LineString[]::new)).toText();
    }

    default <K extends Knife<K>> Slice.Upper<K> upper(K lower) {
        return new Slice.Upper<>(lower);
    }

    default Knife.X x(double value) {
        return new Knife.X(value, Ginsu.DEFAULT_OFFSET, 0D);
    }

    default List<Knife.X> x(double[] values) {
        return Ginsu.map(values, v -> new Knife.X(v, Ginsu.DEFAULT_OFFSET, 0D));
    }

    default Knife.Y y(double value) {
        return new Knife.Y(value, Ginsu.DEFAULT_OFFSET, 0D);
    }

    default List<Knife.Y> y(double[] values) {
        return Ginsu.map(values, v -> new Knife.Y(v, Ginsu.DEFAULT_OFFSET, 0D));
    }
}
