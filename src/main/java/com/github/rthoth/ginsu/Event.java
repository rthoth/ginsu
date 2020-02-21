package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

import java.util.Comparator;
import java.util.Objects;

public abstract class Event {

	public static final Comparator<Event> COMPARATOR = (e1, e2) ->
		(e1 != e2) ? Double.compare(e1.ordinate, e2.ordinate) : 0;

	protected final IndexedCoordinateSequence sequence;
	protected final int index;
	protected final int position;
	protected final Coordinate coordinate;
	protected final double ordinate;

	public Event(IndexedCoordinateSequence sequence, int index, int position, double ordinate, Coordinate coordinate) {
		assert position != 0 : "Invalid position " + position + "!";
		this.sequence = sequence;
		this.index = index;
		this.position = position;
		this.coordinate = coordinate;
		this.ordinate = ordinate;
	}

	public int getIndex() {
		return index;
	}

	public String getText() {
		return "(" +
			index +
			", " +
			position +
			", " +
			ordinate +
			(coordinate != null ? ", (" + coordinate.getX() + ", " + coordinate.getY() + ")" : ", null") +
			")";
	}

	public Coordinate getCoordinate() {
		return coordinate != null ? coordinate : sequence.get(index);
	}

	public CoordinateSequence getCoordinateSequence() {
		return sequence.getCoordinateSequence();
	}

	public static class Factory {

		private final IndexedCoordinateSequence indexedCoordinateSequence;

		public Factory(IndexedCoordinateSequence indexedCoordinateSequence) {
			this.indexedCoordinateSequence = indexedCoordinateSequence;
		}

		public In newIn(int index, int position, double ordinate, Coordinate coordinate) {
			return new In(indexedCoordinateSequence, index, position, ordinate, coordinate);
		}

		public Out newOut(int index, int position, double ordinate, Coordinate coordinate) {
			return new Out(indexedCoordinateSequence, index, position, ordinate, coordinate);
		}

		public IndexedCoordinateSequence getIndexedCoordinateSequence() {
			return indexedCoordinateSequence;
		}
	}

	public static class In extends Event {

		public In(IndexedCoordinateSequence indexedCoordinateSequence, int index, int position, double ordinate, Coordinate coordinate) {
			super(indexedCoordinateSequence, index, position, ordinate, coordinate);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof In) {
				In other = (In) obj;
				return other.index == index && other.position == position && Objects.equals(coordinate, other.coordinate);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return "In" + getText();
		}
	}

	public static class Out extends Event {

		public Out(IndexedCoordinateSequence indexedCoordinateSequence, int index, int position, double ordinate, Coordinate coordinate) {
			super(indexedCoordinateSequence, index, position, ordinate, coordinate);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Out) {
				Out other = (Out) obj;
				return other.index == index && other.position == position && Objects.equals(coordinate, other.coordinate);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return "Out" + getText();
		}
	}
}
