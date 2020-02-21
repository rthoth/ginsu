package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.Empty;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public abstract class Cell<K extends Knife<K>> {

	public abstract int positionOf(Coordinate coordinate);

	public abstract Coordinate intersection(Coordinate previous, Coordinate currentCoordinate, int position);

	public abstract double ordinateOf(Coordinate coordinate);

	public static class Lower<K extends Knife<K>> extends Cell<K> {

		private final K upper;

		public Lower(K upper) {
			this.upper = upper;
		}

		@Override
		public Coordinate intersection(Coordinate origin, Coordinate target, int position) {
			if (position == 1 || position == 2) {
				return upper.intersection(origin, target);
			} else {
				throw new IllegalArgumentException(String.valueOf(position));
			}
		}

		@Override
		public double ordinateOf(Coordinate coordinate) {
			return upper.ordinateOf(coordinate);
		}

		@Override
		public int positionOf(Coordinate coordinate) {
			switch (upper.positionOf(coordinate)) {
				case 1:
					return 2;

				case -1:
					return 0;

				case 0:
					return 1;

				default:
					throw new IllegalStateException(String.valueOf(upper.positionOf(coordinate)));
			}
		}

		@Override
		public String toString() {
			return "Lower(" + upper + ")";
		}
	}

	public static class Middle<K extends Knife<K>> extends Cell<K> {

		private final K lower;

		private final K upper;

		public Middle(K lower, K upper) {
			assert lower.compareTo(upper) < 0 : "Invalid: " + lower + " < " + upper;
			this.lower = lower;
			this.upper = upper;
		}

		@Override
		public Coordinate intersection(Coordinate origin, Coordinate target, int position) {
			if (position == -2 || position == -1) {
				return lower.intersection(origin, target);
			} else if (position == 2 || position == 1) {
				return upper.intersection(origin, target);
			} else {
				throw new IllegalArgumentException(String.valueOf(position));
			}
		}

		@Override
		public double ordinateOf(Coordinate coordinate) {
			return upper.ordinateOf(coordinate);
		}

		@Override
		public int positionOf(Coordinate coordinate) {
			switch (lower.positionOf(coordinate)) {
				case -1:
					return -2;

				case 0:
					return -1;
			}

			switch (upper.positionOf(coordinate)) {
				case 1:
					return 2;

				case 0:
					return 1;
			}

			return 0;
		}

		@Override
		public String toString() {
			return "Middle(" + lower + ", " + upper + ")";
		}
	}

	public static class Upper<K extends Knife<K>> extends Cell<K> {

		private final K lower;

		public Upper(K lower) {
			this.lower = lower;
		}

		@Override
		public Coordinate intersection(Coordinate origin, Coordinate target, int position) {
			if (position == -2 || position == -1) {
				return lower.intersection(origin, target);
			} else {
				throw new IllegalArgumentException(String.valueOf(position));
			}
		}

		@Override
		public double ordinateOf(Coordinate coordinate) {
			return lower.ordinateOf(coordinate);
		}

		@Override
		public int positionOf(Coordinate coordinate) {
			switch (lower.positionOf(coordinate)) {
				case -1:
					return -2;

				case 1:
					return 0;

				case 0:
					return -1;

				default:
					throw new IllegalStateException(String.valueOf(lower.positionOf(coordinate)));
			}
		}

		@Override
		public String toString() {
			return "Upper(" + lower + ")";
		}
	}

	public static <K extends Knife<K>> PVector<Cell<K>> create(Iterable<K> knives) {
		var it = knives.iterator();

		if (it.hasNext()) {
			var previous = it.next();
			var current = previous;
			var ret = TreePVector.<Cell<K>>singleton(new Lower<>(previous.getUpper()));

			while (it.hasNext()) {
				current = it.next();
				ret = ret.plus(new Middle<>(previous.getLower(), current.getUpper()));
				previous = current;
			}

			return ret.plus(new Upper<>(previous.getLower()));

		} else {
			return Empty.vector();
		}
	}
}
