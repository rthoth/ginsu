package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;

public class EventTest {

	public static Event.In in(IndexedCoordinateSequence sequence, int index, int position, double ordinate, Coordinate coordinate) {
		return new Event.In(sequence, index, position, ordinate, coordinate);
	}

	public static Event.Out out(IndexedCoordinateSequence sequence, int index, int position, double ordinate, Coordinate coordinate) {
		return new Event.Out(sequence, index, position, ordinate, coordinate);
	}
}
