package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

public abstract class AbstractTest {

    public static final CoordinateSequenceFactory COORDINATE_FACTORY = new CoordinateSequenceFactory() {

        private CoordinateSequence copy(Coordinate[] source, CoordinateSequence target) {
            for (var i = 0; i < source.length; i++) {
                var coord = source[i];
                target.setOrdinate(i, 0, coord.getX());
                target.setOrdinate(i, 1, coord.getY());
            }
            return target;
        }

        private CoordinateSequence copy(CoordinateSequence source, CoordinateSequence target) {
            for (int i = 0, l = target.size(); i < l; i++) {
                var coord = source.getCoordinate(i);
                target.setOrdinate(i, 0, coord.getX());
                target.setOrdinate(i, 1, coord.getY());
            }

            return target;
        }

        @Override
        public CoordinateSequence create(CoordinateSequence coordSeq) {
            return copy(coordSeq, create(coordSeq.size(), 2));
        }

        @Override
        public CoordinateSequence create(Coordinate[] coordinates) {
            return copy(coordinates, create(coordinates.length, 2));
        }

        @Override
        public CoordinateSequence create(int size, int dimension) {
            return new PackedCoordinateSequence.Double(size, 2, 0);
        }
    };

    public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public Stream<MultiPolygon> loadMultiPolygonStream(File file) {
        try {
            return Files.lines(file.toPath()).map(this::parseMultiPolygon);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MultiPolygon parseMultiPolygon(String wkt) {
        return (MultiPolygon) parseWKT(wkt);
    }

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
