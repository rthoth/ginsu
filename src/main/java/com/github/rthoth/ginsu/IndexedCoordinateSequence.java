package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

public class IndexedCoordinateSequence {

	private final int index;
	private final CoordinateSequence sequence;

	public IndexedCoordinateSequence(CoordinateSequence sequence, int index) {
		this.sequence = sequence;
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public CoordinateSequence getCoordinateSequence() {
		return sequence;
	}

	public Coordinate get(int index) {
		return sequence.getCoordinate(index);
	}

	public boolean isClosed() {
		return sequence.getCoordinate(0).equals2D(sequence.getCoordinate(sequence.size() - 1));
	}

	public int size() {
		return sequence.size();
	}
}
