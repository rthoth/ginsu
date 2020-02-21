package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeometryTest {

	public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PackedCoordinateSequenceFactory(PackedCoordinateSequenceFactory.DOUBLE));

	public static Coordinate coordinate(double x, double y) {
		return new CoordinateXY(x, y);
	}

	@SuppressWarnings("unchecked")
	public static <G extends Geometry> G wkt(String wkt) {
		var reader = new WKTReader(GEOMETRY_FACTORY);
		reader.setIsOldJtsCoordinateSyntaxAllowed(false);

		try {
			return (G) reader.read(wkt);
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}
}
