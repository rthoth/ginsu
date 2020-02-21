package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;

public abstract class Knife<K extends Knife<K>> implements Comparable<K> {

	protected final double position;
	protected final double offset;
	protected final K lower;
	protected final K upper;

	protected Knife(double position, double offset, K upper, K lower) {
		this.offset = Math.abs(offset);
		this.position = position;
		this.lower = lower;
		this.upper = upper;
	}

	public abstract K extrude(double extrusion);

	public abstract K getLower();

	public abstract K getUpper();

	public abstract int positionOf(Coordinate coordinate);

	public abstract Coordinate intersection(Coordinate origin, Coordinate target);

	public abstract double ordinateOf(Coordinate coordinate);

	public static class X extends Knife<X> {

		public X(double position, double offset) {
			super(position, offset, null, null);
		}

		public X(double position, double offset, double extrusion) {
			super(position, offset, new X(position - extrusion, offset), new X(position + extrusion, offset));
		}

		@Override
		public int compareTo(X x) {
			return Double.compare(position, x.position);
		}

		@Override
		public X getLower() {
			return lower == null ? this : lower;
		}

		@Override
		public X getUpper() {
			return upper == null ? this : upper;
		}

		@Override
		public X extrude(double extrusion) {
			return new X(position, offset, extrusion);
		}

		@Override
		public double ordinateOf(Coordinate coordinate) {
			return coordinate.getY();
		}

		@Override
		public int positionOf(Coordinate coordinate) {
			return Math.abs(position - coordinate.getX()) > offset ? Double.compare(coordinate.getX(), position) : 0;
		}

		@Override
		public Coordinate intersection(Coordinate origin, Coordinate target) {
			final double x0 = origin.getX(), y0 = origin.getY();
			final double x1 = target.getX(), y1 = target.getY();
			final double d = x1 - x0;

			if (d != 0D) {
				return new Coordinate(position, y0 + ((position - x0) / (x1 - x0)) * (y1 - y0));
			} else {
				throw new ArithmeticException();
			}
		}

		@Override
		public String toString() {
			return "X(" + position + " +/- " + offset + ")";
		}
	}

	public static class Y extends Knife<Y> {

		public Y(double position, double offset) {
			super(position, offset, null, null);
		}

		public Y(double position, double offset, double extrusion) {
			super(position, offset, new Y(position - extrusion, offset), new Y(position + extrusion, offset));
		}

		@Override
		public int compareTo(Y y) {
			return Double.compare(position, y.position);
		}

		@Override
		public Y getLower() {
			return lower == null ? this : lower;
		}

		@Override
		public Y getUpper() {
			return upper == null ? this : upper;
		}

		@Override
		public Y extrude(double extrusion) {
			return new Y(position, offset, extrusion);
		}

		@Override
		public double ordinateOf(Coordinate coordinate) {
			return coordinate.getX();
		}

		@Override
		public int positionOf(Coordinate coordinate) {
			return Math.abs(position - coordinate.getY()) > offset ? Double.compare(coordinate.getY(), position) : 0;
		}

		@Override
		public Coordinate intersection(Coordinate origin, Coordinate target) {
			final double x0 = origin.getX(), y0 = origin.getY();
			final double x1 = target.getX(), y1 = target.getY();
			final double d = y1 - y0;

			if (d != 0D) {
				return new Coordinate(x0 + ((position - y0) / (y1 - y0)) * (x1 - x0), position);
			} else {
				throw new ArithmeticException();
			}
		}

		@Override
		public String toString() {
			return "Y(" + position + " +/- " + offset + ")";
		}
	}
}
