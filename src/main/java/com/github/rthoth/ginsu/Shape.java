package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Iterator;

public abstract class Shape implements Iterable<IndexedCoordinateSequence> {

	public static final Shape EMPTY = new Empty();

	public abstract boolean nonEmpty();

	public static Shape of(CoordinateSequence... sequences) {
		var index = 0;
		var ret = TreePVector.<IndexedCoordinateSequence>empty();

		for (var sequence : sequences) {
			if (sequence.size() > 0)
				ret = ret.plus(new IndexedCoordinateSequence(sequence, index++));
		}

		return index > 0 ? new NotEmpty(ret) : EMPTY;
	}

	public static Shape of(Iterable<CoordinateSequence> iterable) {
		return of(iterable.iterator());
	}

	public static Shape of(Iterator<CoordinateSequence> iterator) {
		var index = 0;
		var sequences = TreePVector.<IndexedCoordinateSequence>empty();
		while (iterator.hasNext()) {
			var sequence = iterator.next();
			if (sequence.size() > 0) {
				index++;
				sequences = sequences.plus(new IndexedCoordinateSequence(sequence, index));
			}
		}

		return index > 0 ? new NotEmpty(sequences) : EMPTY;
	}

	public static Shape of(@NotNull Polygon polygon) {
		if (!polygon.isEmpty()) {

			PVector<IndexedCoordinateSequence> sequences = TreePVector
				.singleton(new IndexedCoordinateSequence(polygon.getExteriorRing().getCoordinateSequence(), 0));

			var index = 1;
			for (int i = 0, l = polygon.getNumInteriorRing(); i < l; i++) {
				var hole = polygon.getInteriorRingN(i).getCoordinateSequence();
				if (hole.size() > 0)
					sequences = sequences.plus(new IndexedCoordinateSequence(hole, index++));
			}

			return new NotEmpty(sequences);
		} else {
			return EMPTY;
		}
	}

	public static Shape of(@NotNull LineString lineString) {
		if (!lineString.isEmpty()) {
			return new NotEmpty(TreePVector.singleton(new IndexedCoordinateSequence(lineString.getCoordinateSequence(), 0)));
		} else {
			return EMPTY;
		}
	}

	private static class NotEmpty extends Shape {

		private final PVector<IndexedCoordinateSequence> sequences;

		public NotEmpty(@NotNull PVector<IndexedCoordinateSequence> sequences) {
			this.sequences = sequences;
		}

		@Override
		public Iterator<IndexedCoordinateSequence> iterator() {
			return sequences.iterator();
		}

		@Override
		public boolean nonEmpty() {
			return true;
		}
	}

	private static class Empty extends Shape {

		@Override
		public boolean nonEmpty() {
			return false;
		}

		@Override
		public Iterator<IndexedCoordinateSequence> iterator() {
			return Collections.emptyIterator();
		}
	}
}
