package com.github.rthoth.ginsu;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Collections;
import java.util.Iterator;

public abstract class ShapeDetection implements Iterable<Detection> {

	protected final Shape shape;

	public ShapeDetection(Shape shape) {
		this.shape = shape;
	}

	public MultiShape getSourceAsMultiShape() {
		return MultiShape.of(shape);
	}

	public abstract boolean nonEmpty();

	public static class Root implements Aggregation.Root<Detection, ShapeDetection> {

		private final Shape shape;

		public Root(Shape shape) {
			this.shape = shape;
		}

		@Override
		public Aggregation<Detection, ShapeDetection> begin() {
			return new Agg(shape, TreePVector.empty());
		}

		@Override
		public Aggregation<Detection, ShapeDetection> append(Detection element) {
			return new Agg(shape, TreePVector.singleton(element));
		}
	}

	private static class Agg implements Aggregation<Detection, ShapeDetection> {

		private final Shape shape;
		private final PVector<Detection> detections;

		public Agg(Shape shape, PVector<Detection> detections) {
			this.shape = shape;
			this.detections = detections;
		}

		@Override
		public ShapeDetection aggregate() {
			return !detections.isEmpty() ? new NotEmpty(shape, detections) : new Empty(shape);
		}

		@Override
		public Aggregation<Detection, ShapeDetection> append(Detection element) {
			return new Agg(shape, detections.plus(element));
		}
	}

	private static class Empty extends ShapeDetection {

		public Empty(Shape shape) {
			super(shape);
		}

		@Override
		public boolean nonEmpty() {
			return false;
		}

		@Override
		public Iterator<Detection> iterator() {
			return Collections.emptyIterator();
		}
	}

	private static class NotEmpty extends ShapeDetection {

		private final PVector<Detection> detections;

		private NotEmpty(Shape shape, PVector<Detection> detections) {
			super(shape);
			this.detections = detections;
		}

		@Override
		public Iterator<Detection> iterator() {
			return detections.iterator();
		}

		public boolean nonEmpty() {
			return true;
		}

	}
}
