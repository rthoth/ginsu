package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Segment.Backward;
import com.github.rthoth.ginsu.Segment.Forward;
import com.github.rthoth.ginsu.Segment.LineSegment;
import com.github.rthoth.ginsu.Segment.PointSegment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Envelope;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class SegmentedCoordinateSequence implements CoordinateSequence {

	private final NavigableMap<IntRange, Segment> pMap = new TreeMap<>();
	private final int size;
	private final int dimension;

	public SegmentedCoordinateSequence(int dimension, PVector<Segment> segments) {
		this.dimension = dimension;
		var size = 0;
		var newSize = 0;

		for (var segment : segments) {
			newSize += segment.size();
			pMap.put(IntRange.of(size, newSize - 1), segment.withBottom(size));
			size = newSize;
		}

		this.size = size;
	}

	protected Segment getSegment(int index) {
		var segment = pMap.get(IntRange.of(index));
		if (segment != null) {
			return segment;
		} else {
			throw new IndexOutOfBoundsException(String.valueOf(index));
		}
	}

	@Override
	public int getDimension() {
		return dimension;
	}

	@Override
	public Coordinate getCoordinate(int index) {
		return getSegment(index).getCoordinate(index);
	}

	@Override
	public Coordinate getCoordinateCopy(int index) {
		return getSegment(index).getCoordinateCopy(index);
	}

	@Override
	public void getCoordinate(int index, Coordinate coordinate) {
		getSegment(index).getCoordinate(index, coordinate);
	}

	@Override
	public double getX(int index) {
		return getSegment(index).getX(index);
	}

	@Override
	public double getY(int index) {
		return getSegment(index).getY(index);
	}

	@Override
	public double getOrdinate(int index, int ordinateIndex) {
		return getSegment(index).getOrdinate(index, ordinateIndex);
	}

	@Override
	public void setOrdinate(int index, int ordinateIndex, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Coordinate[] toCoordinateArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Envelope expandEnvelope(Envelope env) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CoordinateSequence copy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return CoordinateSequences.toString(this);
	}

	public static class Builder {

		private final int dimension;
		private PVector<Segment> segments = TreePVector.empty();

		public Builder(int dimension) {
			this.dimension = dimension;
		}

		public Optional<CoordinateSequence> close() {
			var segments = this.segments;

			if (segments.size() > 1) {
				var first = segments.get(0);
				var last = segments.get(segments.size() - 1);

				var opening = first.getCoordinate(0);
				if (!opening.equals2D(last.getCoordinate(last.size() - 1))) {
					segments = segments.plus(new PointSegment(opening, 0));
				}

			} else if (segments.size() == 1) {
				var segment = segments.get(0);
				if (!segment.getCoordinate(0).equals2D(segment.getCoordinate(segment.size() - 1))) {
					segments = segments.plus(new PointSegment(segment.getCoordinate(0), 0));
				}
			}

			var size = segments.stream().mapToInt(Segment::size).sum();
			return size != 0 ? Optional.of(new SegmentedCoordinateSequence(dimension, segments)) : Optional.empty();
		}

		public Builder forward(CoordinateSequence sequence, Event start, Event stop) {
			if (start.index >= 0 && stop.index >= 0) {

				if (start.coordinate != null)
					segments = segments.plus(new PointSegment(start.coordinate, 0));

				segments = segments.plus(Forward.create(sequence, start.index, stop.index));

				if (stop.coordinate != null)
					segments = segments.plus(new PointSegment(stop.coordinate, 0));

			} else if (start.index == -1 && stop.index == -1) {
				segments = segments.plus(new LineSegment(0, start.coordinate, stop.coordinate));
			} else {
				throw new IllegalStateException();
			}

			return this;
		}

		public Builder backward(CoordinateSequence sequence, Event start, Event stop) {
			if (start.index >= 0 && stop.index >= 0) {

				if (start.coordinate != null)
					segments = segments.plus(new PointSegment(start.coordinate, 0));

				segments = segments.plus(Backward.create(sequence, start.index, stop.index));

				if (stop.coordinate != null)
					segments = segments.plus(new PointSegment(stop.coordinate, 0));

			} else if (start.index == -1 && stop.index == -1) {
				segments = segments.plus(new LineSegment(0, start.coordinate, stop.coordinate));
			} else {
				throw new IllegalStateException();
			}

			return this;
		}
	}
}
