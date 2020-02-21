package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.TopologyException;

public abstract class Segment {

	protected final int bottom;

	public Segment(int bottom) {
		if (bottom < 0)
			throw new IllegalArgumentException("Invalid bottom " + bottom + "!");

		this.bottom = bottom;
	}

	public abstract int size();

	public abstract Segment withBottom(int bottom);

	public abstract Coordinate getCoordinate(int index);

	public abstract Coordinate getCoordinateCopy(int index);

	public abstract void getCoordinate(int index, Coordinate coordinate);

	public abstract double getX(int index);

	public abstract double getY(int index);

	public abstract double getOrdinate(int index, int ordinateIndex);

	public abstract static class View extends Segment {

		protected final CoordinateSequence sequence;
		protected final int size;
		protected final int start;
		protected final int limit;

		public View(CoordinateSequence sequence, int bottom, int start, int size, int limit) {
			super(bottom);
			this.sequence = sequence;
			this.size = size;
			this.start = start;
			this.limit = limit;
		}

		@Override
		public int size() {
			return size;
		}

		protected abstract int computeIndex(int index);

		protected int mapIndex(int index) {
			var validIndex = index - bottom;
			if (validIndex < size)
				return computeIndex(validIndex);
			else
				throw new IndexOutOfBoundsException(index);
		}

		@Override
		public Coordinate getCoordinate(int index) {
			return sequence.getCoordinate(mapIndex(index));
		}

		@Override
		public Coordinate getCoordinateCopy(int index) {
			return sequence.getCoordinateCopy(mapIndex(index));
		}

		@Override
		public void getCoordinate(int index, Coordinate coordinate) {
			sequence.getCoordinate(mapIndex(index), coordinate);
		}

		@Override
		public double getX(int index) {
			return sequence.getX(mapIndex(index));
		}

		@Override
		public double getY(int index) {
			return sequence.getY(mapIndex(index));
		}

		@Override
		public double getOrdinate(int index, int ordinateIndex) {
			return sequence.getOrdinate(mapIndex(index), ordinateIndex);
		}
	}

	public static class Forward extends View {

		public Forward(CoordinateSequence sequence, int bottom, int start, int size, int limit) {
			super(sequence, bottom, start, size, limit);
		}

		public static Segment create(CoordinateSequence sequence, int start, int stop) {
			if (start != stop) {
				var isRing = CoordinateSequences.isRing(sequence);
				var limit = isRing ? sequence.size() - 1 : sequence.size();

				if (start < stop)
					return new Forward(sequence, 0, start, stop - start + 1, limit);
				else if (isRing)
					return new Forward(sequence, 0, start, stop + limit - start + 1, limit);
				else
					throw new TopologyException("Sequence must be a ring!");
			} else {
				return new PointSegment(sequence.getCoordinate(start), 0);
			}
		}

		@Override
		protected int computeIndex(int index) {
			return (start + index) % limit;
		}

		@Override
		public Segment withBottom(int bottom) {
			return new Forward(sequence, bottom, start, size, limit);
		}
	}

	public static class Backward extends View {

		public Backward(CoordinateSequence sequence, int bottom, int start, int size, int limit) {
			super(sequence, bottom, start, size, limit);
		}

		public static Segment create(CoordinateSequence sequence, int start, int stop) {
			if (start != stop) {
				var isRing = CoordinateSequences.isRing(sequence);
				var limit = isRing ? sequence.size() - 1 : sequence.size();

				if (start > stop)
					return new Backward(sequence, 0, start, start - stop + 1, limit);
				else if (isRing)
					return new Backward(sequence, 0, start, start + limit - stop + 1, limit);
				else
					throw new TopologyException("Sequence must be a ring!");
			} else {
				return new PointSegment(sequence.getCoordinate(start), 0);
			}
		}

		@Override
		protected int computeIndex(int index) {
			return index <= start ? start - index : limit + ((start - index) % limit);
		}

		@Override
		public Segment withBottom(int bottom) {
			return new Backward(sequence, bottom, start, size, limit);
		}
	}

	public static class LineSegment extends Segment {

		private final Coordinate _0;
		private final Coordinate _1;

		public LineSegment(int bottom, Coordinate _0, Coordinate _1) {
			super(bottom);
			this._0 = _0;
			this._1 = _1;
		}

		private int mapIndex(int index) {
			var mapped = index - bottom;
			if (mapped == 0 || mapped == 1)
				return mapped;
			else
				throw new IndexOutOfBoundsException(index);
		}

		@Override
		public Coordinate getCoordinate(int index) {
			return mapIndex(index) == 0 ? _0 : _1;
		}

		@Override
		public Coordinate getCoordinateCopy(int index) {
			return getCoordinate(index).copy();
		}

		@Override
		public void getCoordinate(int index, Coordinate coordinate) {
			coordinate.setCoordinate(getCoordinate(index));
		}

		@Override
		public double getX(int index) {
			return getCoordinate(index).getX();
		}

		@Override
		public double getY(int index) {
			return getCoordinate(index).getY();
		}

		@Override
		public double getOrdinate(int index, int ordinateIndex) {
			return getCoordinate(index).getOrdinate(ordinateIndex);
		}

		@Override
		public int size() {
			return 2;
		}

		@Override
		public Segment withBottom(int bottom) {
			return new LineSegment(bottom, _0, _1);
		}
	}

	public static class PointSegment extends Segment {

		private final Coordinate coordinate;

		public PointSegment(Coordinate coordinate, int bottom) {
			super(bottom);
			this.coordinate = coordinate;
		}

		private void checkIndex(int index) {
			if (index - bottom == 0) {
				return;
			}

			throw new IndexOutOfBoundsException(index);
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public Segment withBottom(int bottom) {
			return new PointSegment(coordinate, bottom);
		}

		@Override
		public Coordinate getCoordinate(int index) {
			checkIndex(index);
			return coordinate;
		}

		@Override
		public Coordinate getCoordinateCopy(int index) {
			checkIndex(index);
			return coordinate.copy();
		}

		@Override
		public void getCoordinate(int index, Coordinate coordinate) {
			checkIndex(index);
			coordinate.setCoordinate(this.coordinate);
		}

		@Override
		public double getX(int index) {
			checkIndex(index);
			return coordinate.getX();
		}

		@Override
		public double getY(int index) {
			checkIndex(index);
			return coordinate.getY();
		}

		@Override
		public double getOrdinate(int index, int ordinateIndex) {
			checkIndex(index);
			return coordinate.getOrdinate(ordinateIndex);
		}
	}
}
