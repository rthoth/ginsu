package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import org.locationtech.jts.geom.*;
import org.pcollections.PVector;

import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public class Slicer {

	public static final double DEFAULT_OFFSET = 1e-8;

	private final DetectionGrid grid;
	private final PVector<X> x;
	private final PVector<Y> y;

	public Slicer(@NotNull double[] x, @NotNull double y[]) {
		this(x, y, DEFAULT_OFFSET);
	}

	public Slicer(@NotNull double[] x, @NotNull double y[], double offset) {
		this(Ginsu.mapToVector(x, v -> new X(v, offset)), Ginsu.mapToVector(y, v -> new Y(v, offset)));
	}

	public Slicer(@NotNull PVector<X> x, @NotNull PVector<Y> y) {
		this.x = x;
		this.y = y;
		grid = new DetectionGrid(x, y);
	}

	public Slicer extrude(final double extrusion) {
		if (extrusion != 0D) {
			return new Slicer(Ginsu.mapToVector(x, it -> it.extrude(extrusion)), Ginsu.mapToVector(y, it -> it.extrude(extrusion)));
		} else {
			return this;
		}
	}

	public Grid<? extends GeometryCollection> apply(Geometry geometry) {
		return apply(geometry, Order.AUTOMATIC);
	}

	public Grid<? extends GeometryCollection> apply(Geometry geometry, Order order) {
		if (geometry instanceof Polygon) {
			return apply((Polygon) geometry, order);
		} else if (geometry instanceof MultiPolygon) {
			return apply((MultiPolygon) geometry, order);
		} else if (geometry instanceof LineString) {
			return apply((LineString) geometry, order);
		} else if (geometry instanceof MultiLineString) {
			return apply((MultiLineString) geometry, order);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public Grid<MultiPolygon> apply(Polygon polygon) {
		return apply(polygon, Order.AUTOMATIC);
	}

	public Grid<MultiPolygon> apply(Polygon polygon, Order order) {
		return new PolygonSlicer(polygon, order, grid).getGrid();
	}

	public Grid<MultiPolygon> apply(MultiPolygon multiPolygon) {
		return apply(multiPolygon, Order.AUTOMATIC);
	}

	public Grid<MultiPolygon> apply(MultiPolygon multiPolygon, Order order) {
		return new PolygonSlicer(multiPolygon, order, grid).getGrid();
	}

	public Grid<MultiLineString> apply(LineString lineString) {
		return apply(lineString, Order.AUTOMATIC);
	}

	public Grid<MultiLineString> apply(LineString lineString, Order order) {
		return new LineStringSlicer(lineString, order, grid).getGrid();
	}

	public Grid<MultiLineString> apply(MultiLineString multiLineString) {
		return apply(multiLineString, Order.AUTOMATIC);
	}

	public Grid<MultiLineString> apply(MultiLineString multiLineString, Order order) {
		return new LineStringSlicer(multiLineString, order, grid).getGrid();
	}
}
