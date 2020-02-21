package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.*;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Iterator;

public abstract class MultiShape implements Iterable<Shape> {

	public static final MultiShape EMPTY = new Empty();

	public static final Aggregation.Root<MultiShape, MultiShape> MULTI_ROOT = new Aggregation.Root<>() {

		@Override
		public Aggregation<MultiShape, MultiShape> begin() {
			return new MultiAgg(TreePVector.empty());
		}

		@Override
		public Aggregation<MultiShape, MultiShape> append(MultiShape element) {
			if (element instanceof NotEmpty) {
				return new MultiAgg(TreePVector.<Shape>empty().plusAll(((NotEmpty) element).shapes));
			} else {
				return new MultiAgg(TreePVector.empty());
			}
		}
	};

	public abstract boolean nonEmpty();

	public static MultiShape of(Iterable<Shape> shapes) {
		var vector = TreePVector.<Shape>empty();
		for (var shape : shapes) {
			if (shape.nonEmpty())
				vector = vector.plus(shape);
		}

		return !vector.isEmpty() ? new NotEmpty(vector) : EMPTY;
	}

	public static MultiShape of(Shape shape) {
		return shape.nonEmpty() ? new NotEmpty(TreePVector.singleton(shape)) : EMPTY;
	}

	public static MultiShape of(LineString lineString) {
		if (!lineString.isEmpty())
			return new NotEmpty(TreePVector.singleton(Shape.of(lineString)));
		else
			return EMPTY;
	}

	public static MultiShape of(MultiLineString multiLineString) {
		if (!multiLineString.isEmpty()) {
			PVector<Shape> vector = Ginsu
				.mapToVector(Ginsu.components(multiLineString), g -> Shape.of((LineString) g));

			return new NotEmpty(vector);
		} else
			return EMPTY;
	}

	public static MultiShape of(MultiPolygon multiPolygon) {
		if (!multiPolygon.isEmpty()) {
			PVector<Shape> vector = Ginsu
				.mapToVector(Ginsu.components(multiPolygon), g -> Shape.of((Polygon) g));

			return new NotEmpty(vector);
		} else
			return EMPTY;
	}

	public static MultiShape of(Polygon polygon) {
		if (!polygon.isEmpty()) {
			return new NotEmpty(TreePVector.singleton(Shape.of(polygon)));
		} else
			return EMPTY;
	}

	public static MultiPolygon toMultiPolygon(MultiShape multiShape, GeometryFactory factory) {
		if (multiShape.nonEmpty()) {
			return factory
				.createMultiPolygon(Ginsu.mapToArray(multiShape, shape -> toPolygon(shape, factory), Polygon[]::new));
		} else {
			return factory.createMultiPolygon();
		}
	}

	public static Polygon toPolygon(Shape shape, GeometryFactory factory) {
		if (shape.nonEmpty()) {
			var it = shape.iterator();
			var shell = factory.createLinearRing(Ginsu.next(it).getCoordinateSequence());
			var holes = Ginsu.mapToArray(it, list -> factory.createLinearRing(list.getCoordinateSequence()), LinearRing[]::new);
			return factory.createPolygon(shell, holes);
		} else {
			return factory.createPolygon();
		}
	}

	private static class MultiAgg implements Aggregation<MultiShape, MultiShape> {

		private final PVector<Shape> shapes;

		public MultiAgg(PVector<Shape> shapes) {
			this.shapes = shapes;
		}

		@Override
		public MultiShape aggregate() {
			return !shapes.isEmpty() ? new NotEmpty(shapes) : EMPTY;
		}

		@Override
		public Aggregation<MultiShape, MultiShape> append(MultiShape element) {
			if (element instanceof NotEmpty) {
				return new MultiAgg(shapes.plusAll(((NotEmpty) element).shapes));
			} else {
				return this;
			}
		}
	}

	private static class Empty extends MultiShape {

		@Override
		public Iterator<Shape> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public boolean nonEmpty() {
			return false;
		}
	}

	private static class NotEmpty extends MultiShape {

		private final PVector<Shape> shapes;

		public NotEmpty(PVector<Shape> shapes) {
			this.shapes = shapes;
		}

		@Override
		public boolean nonEmpty() {
			return true;
		}

		@Override
		public Iterator<Shape> iterator() {
			return shapes.iterator();
		}
	}
}
