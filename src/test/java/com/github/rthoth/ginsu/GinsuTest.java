package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public interface GinsuTest {

    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    default double[] array(double... values) {
        return values;
    }

    default Polygon parsePolygon(String wkt) {
        try {
            return (Polygon) new WKTReader(GEOMETRY_FACTORY).read(wkt);
        } catch (ParseException e) {
            throw new GinsuException.ParseException("!", e);
        }
    }
}
