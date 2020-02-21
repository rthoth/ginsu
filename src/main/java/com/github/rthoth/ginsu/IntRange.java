package com.github.rthoth.ginsu;

import java.util.Objects;

public abstract class IntRange implements Comparable<IntRange> {

	public static IntRange of(int value) {
		return new Point(value);
	}

	public static IntRange of(int from, int to) {
		return from != to ? new Lane(from, to) : new Point(from);
	}

	public static class Lane extends IntRange {

		private final int from;
		private final int to;

		public Lane(int from, int to) {
			if (from >= to)
				throw new IllegalArgumentException(from + " - " + to);
			this.from = from;
			this.to = to;
		}

		@Override
		public int compareTo(IntRange o) {
			if (o instanceof Lane) {
				return compareTo((Lane) o);
			} else if (o instanceof Point) {
				return compareTo((Point) o);
			} else
				throw new IllegalArgumentException(Objects.toString(o));
		}

		private int compareTo(Lane lane) {
			if (to <= lane.from)
				return -1;
			else
				return from >= lane.to ? 1 : 0;
		}

		private int compareTo(Point point) {
			if (to < point.value)
				return -1;
			else
				return from > point.value ? 1 : 0;
		}

		@Override
		public String toString() {
			return "(" + from + ", " + to + ")";
		}
	}

	public static class Point extends IntRange {

		private final int value;

		public Point(int value) {
			this.value = value;
		}

		@Override
		public int compareTo(IntRange o) {
			if (o instanceof Point)
				return compareTo((Point) o);
			else if (o instanceof Lane)
				return compareTo((Lane) o);
			else
				throw new IllegalArgumentException(Objects.toString(o));
		}

		private int compareTo(Point point) {
			return Double.compare(value, point.value);
		}

		private int compareTo(Lane lane) {
			if (value < lane.from)
				return -1;
			else
				return value > lane.to ? 1 : 0;
		}

		@Override
		public String toString() {
			return "(" + value + ")";
		}
	}
}
