package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

@SuppressWarnings("unused")
public abstract class AbstractTest {

    public static final CoordinateSequenceFactory COORDINATE_FACTORY = new PackedCoordinateSequenceFactory(PackedCoordinateSequenceFactory.DOUBLE);

    public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(COORDINATE_FACTORY);

    public Polygon parsePolygon(String wkt) {
        return (Polygon) parseWKT(wkt);
    }

    public CoordinateSequence parseSequence(String sequence) {
        return ((LineString) parseWKT(String.format("LINESTRING %s", sequence))).getCoordinateSequence();
    }

    public Geometry parseWKT(String wkt) {
        try {
            var reader = new WKTReader(GEOMETRY_FACTORY);
            reader.setIsOldJtsCoordinateSyntaxAllowed(false);
            return reader.read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
