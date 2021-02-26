package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.pcollections.PVector;

public interface GinsuTest {

    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    default double[] array(double... values) {
        return values;
    }

    default MultiPolygon parseMultiPolygon(String wkt) {
        try {
            return (MultiPolygon) new WKTReader(GEOMETRY_FACTORY).read(wkt);
        } catch (ParseException e) {
            throw new GinsuException.ParseException("!", e);
        }
    }

    @SuppressWarnings("unused")
    default Polygon parsePolygon(String wkt) {
        try {
            return (Polygon) new WKTReader(GEOMETRY_FACTORY).read(wkt);
        } catch (ParseException e) {
            throw new GinsuException.ParseException("!", e);
        }
    }

    default Shape shape(String... coordinates) {
        var sequences = Ginsu.toVector(coordinates,
                                       str -> {
                                           try {
                                               return ((LineString) new WKTReader(GEOMETRY_FACTORY).read(String.format("LINESTRING (%s)", str)));
                                           } catch (ParseException e) {
                                               throw new RuntimeException(e);
                                           }
                                       });
        return Shape.of(GEOMETRY_FACTORY.createMultiLineString(sequences.toArray(LineString[]::new)));
    }

    default PVector<Knife.X> xs(double... values) {
        return Ginsu.toVector(values, v -> new Knife.X(v, Ginsu.DEFAULT_OFFSET, 0D));
    }

    default Knife.Y y(double value) {
        return new Knife.Y(value, Ginsu.DEFAULT_OFFSET, 0D);
    }

    default PVector<Knife.Y> ys(double... values) {
        return Ginsu.toVector(values, v -> new Knife.Y(v, Ginsu.DEFAULT_OFFSET, 0D));
    }
}
